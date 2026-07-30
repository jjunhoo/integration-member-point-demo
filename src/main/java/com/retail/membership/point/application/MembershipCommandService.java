package com.retail.membership.point.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.retail.membership.common.lock.DistributedLock;
import com.retail.membership.point.domain.Membership;
import com.retail.membership.point.domain.MembershipRepository;
import com.retail.membership.point.domain.PointLot;
import com.retail.membership.point.domain.PointLotRepository;
import com.retail.membership.point.domain.PointUsage;
import com.retail.membership.point.domain.PointUsageRepository;
import com.retail.membership.point.event.MembershipDomainEvent;
import com.retail.membership.point.event.MembershipEventType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 멤버십 Command 서비스 (CQRS의 쓰기 측).
 *
 * <p>포인트는 lot 단위로 적립되고, 차감은 FEFO(만료 임박 우선)로 lot remaining 을 깎는다.
 * {@code membership.point_balance} 는 사용 가능 잔액 요약이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipCommandService {

    private static final long DEFAULT_EXPIRE_YEARS = 1L;

    private final MembershipRepository membershipRepository;
    private final PointLotRepository pointLotRepository;
    private final PointUsageRepository pointUsageRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @DistributedLock(
            key = "'point:' + #command.userId()",
            waitTime = 3000L,
            leaseTime = 5000L,
            timeUnit = TimeUnit.MILLISECONDS)
    @Transactional
    public void accumulatePoint(PointCommand command) {
        Instant now = Instant.now();
        Membership membership = getOrCreate(command.userId());
        expireStaleLots(membership, now);

        Instant expiresAt = resolveExpiresAt(command.expiresAt(), now);
        PointLot lot = PointLot.earn(command.userId(), command.amount(), now, expiresAt);
        pointLotRepository.save(lot);

        membership.accumulate(command.amount());
        log.debug("[Command] 적립 완료 userId={} amount={} lotId={} expiresAt={} balance={} grade={}",
                command.userId(), command.amount(), lot.getId(), expiresAt,
                membership.getPointBalance(), membership.getGrade());

        publishAfterCommit(membership, MembershipEventType.POINT_ACCUMULATED, command.amount());
    }

    @DistributedLock(
            key = "'point:' + #command.userId()",
            waitTime = 3000L,
            leaseTime = 5000L,
            timeUnit = TimeUnit.MILLISECONDS)
    @Transactional
    public void deductPoint(PointCommand command) {
        Instant now = Instant.now();
        Membership membership = membershipRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 멤버십입니다. userId=" + command.userId()));

        expireStaleLots(membership, now);

        if (membership.getPointBalance() < command.amount()) {
            throw new IllegalStateException(
                    "포인트 잔액이 부족합니다. balance=" + membership.getPointBalance()
                            + ", requested=" + command.amount());
        }

        burnLotsFefo(command.userId(), command.amount(), now);
        membership.deduct(command.amount());

        log.debug("[Command] 차감 완료 userId={} amount={} balance={}",
                command.userId(), command.amount(), membership.getPointBalance());

        publishAfterCommit(membership, MembershipEventType.POINT_DEDUCTED, command.amount());
    }

    /**
     * 유저의 lot 목록(만료·잔여 포함). 조회 시 lazy expire 를 적용하고,
     * 만료분이 있으면 Redis 뷰 동기화를 위해 이벤트를 발행한다.
     */
    @DistributedLock(
            key = "'point:' + #userId",
            waitTime = 3000L,
            leaseTime = 5000L,
            timeUnit = TimeUnit.MILLISECONDS)
    @Transactional
    public List<PointLot> listLots(String userId) {
        Instant now = Instant.now();
        membershipRepository.findById(userId).ifPresent(membership -> {
            long expired = expireStaleLots(membership, now);
            if (expired > 0) {
                publishAfterCommit(membership, MembershipEventType.POINT_EXPIRED, expired);
            }
        });
        return pointLotRepository.findByUserIdOrderByExpiresAtAscEarnedAtAsc(userId);
    }

    private void burnLotsFefo(String userId, long amount, Instant now) {
        List<PointLot> lots = pointLotRepository
                .findByUserIdAndRemainingAmountGreaterThanAndExpiresAtAfterOrderByExpiresAtAscEarnedAtAsc(
                        userId, 0L, now);

        long remainingToBurn = amount;
        String deductTxId = UUID.randomUUID().toString();

        for (PointLot lot : lots) {
            if (remainingToBurn <= 0) {
                break;
            }
            long burned = lot.consume(remainingToBurn);
            if (burned > 0) {
                pointUsageRepository.save(PointUsage.of(userId, lot.getId(), burned, deductTxId, now));
                remainingToBurn -= burned;
            }
        }

        if (remainingToBurn > 0) {
            throw new IllegalStateException(
                    "사용 가능 lot 잔액이 부족합니다. shortfall=" + remainingToBurn);
        }
    }

    /** @return 이번에 만료 처리한 금액 (없으면 0) */
    private long expireStaleLots(Membership membership, Instant now) {
        List<PointLot> expired = pointLotRepository
                .findByUserIdAndRemainingAmountGreaterThanAndExpiresAtLessThanEqual(
                        membership.getUserId(), 0L, now);
        long expiredTotal = 0L;
        for (PointLot lot : expired) {
            expiredTotal += lot.expireRemaining();
        }
        if (expiredTotal > 0) {
            membership.deduct(expiredTotal);
            log.info("[Command] 만료 포인트 정리 userId={} expiredAmount={} balance={}",
                    membership.getUserId(), expiredTotal, membership.getPointBalance());
        }
        return expiredTotal;
    }

    private Instant resolveExpiresAt(Instant requested, Instant earnedAt) {
        if (requested == null) {
            return earnedAt.plus(DEFAULT_EXPIRE_YEARS, ChronoUnit.YEARS);
        }
        if (!requested.isAfter(earnedAt)) {
            throw new IllegalArgumentException("만료일(expiresAt)은 현재 시각보다 이후여야 합니다.");
        }
        return requested;
    }

    private Membership getOrCreate(String userId) {
        return membershipRepository.findById(userId)
                .orElseGet(() -> membershipRepository.save(new Membership(userId)));
    }

    private void publishAfterCommit(Membership membership, MembershipEventType type, long amount) {
        MembershipDomainEvent event = MembershipDomainEvent.of(
                membership.getUserId(),
                type,
                amount,
                membership.getPointBalance(),
                membership.getTotalAccumulatedPoint(),
                membership.getGrade());
        applicationEventPublisher.publishEvent(event);
    }
}
