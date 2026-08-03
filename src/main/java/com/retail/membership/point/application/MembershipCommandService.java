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
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 멤버십 포인트 Command 서비스 (CQRS 쓰기 측).
 *
 * <p><b>용도:</b> 포인트 적립·차감·만료 정리와 lot/usage 원장 갱신을 담당한다.
 * 조회 뷰(Redis)는 직접 쓰지 않고, 커밋 후 도메인 이벤트로 동기화한다.
 *
 * <h3>주요 책임</h3>
 * <ul>
 *   <li>적립: {@code point_lot} 생성 + {@code membership} 잔액/누적/등급 갱신</li>
 *   <li>차감: FEFO(만료 임박 우선)로 lot remaining 차감 + {@code point_usage} 기록</li>
 *   <li>lazy 만료: 만료 lot 잔여 정리 및 잔액 반영 ({@code POINT_EXPIRED} 이벤트)</li>
 *   <li>동시성: {@code @DistributedLock} 으로 동일 userId 요청 직렬화</li>
 *   <li>이벤트: 트랜잭션 커밋 이후 Kafka 발행을 위한 스프링 이벤트 예약</li>
 * </ul>
 *
 * <p>{@code membership.point_balance} 는 사용 가능 lot 잔여 합의 요약 컬럼이다.
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

    /**
     * 포인트 적립.
     *
     * <p>만료 lot 정리 후 새 lot 을 만들고 잔액/누적/등급을 올린다.
     * {@code expiresAt} 미지정 시 적립 시각 + 1년.
     */
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

    /**
     * 포인트 차감(사용).
     *
     * <p>만료 정리 → 잔액 검증 → FEFO lot 소진 → usage 기록 → 잔액 차감 순으로 처리한다.
     */
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

    /**
     * FEFO(만료 임박 → 적립 순)로 사용 가능 lot 의 remaining 을 깎고
     * 동일 {@code deductTxId} 로 {@link PointUsage} 이력을 남긴다.
     *
     * @param userId 대상 유저
     * @param amount 차감할 총액
     * @param now    기준 시각 (만료 판정)
     */
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

    /**
     * 만료된 lot 의 잔여를 0으로 만들고 membership 잔액에서 차감한다.
     *
     * @return 이번에 만료 처리한 금액 (없으면 0)
     */
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

    /**
     * 적립 lot 만료일을 결정한다. 요청값이 없으면 적립 시각 + 1년.
     *
     * @param requested 클라이언트가 넘긴 만료 시각 (nullable)
     * @param earnedAt  적립 시각
     */
    private Instant resolveExpiresAt(Instant requested, Instant earnedAt) {
        if (requested == null) {
            // Instant 는 Years 단위를 지원하지 않음 → UTC OffsetDateTime 으로 1년 가산
            return earnedAt.atOffset(ZoneOffset.UTC).plusYears(DEFAULT_EXPIRE_YEARS).toInstant();
        }
        if (!requested.isAfter(earnedAt)) {
            throw new IllegalArgumentException("만료일(expiresAt)은 현재 시각보다 이후여야 합니다.");
        }
        return requested;
    }

    /** 멤버십이 없으면 신규 생성(잔액 0) 후 반환한다. */
    private Membership getOrCreate(String userId) {
        return membershipRepository.findById(userId)
                .orElseGet(() -> membershipRepository.save(new Membership(userId)));
    }

    /**
     * 변경 후 스냅샷을 담은 도메인 이벤트를 스프링으로 발행한다.
     * 실제 Kafka 전송은 커밋 이후 {@code MembershipEventPublisher} 가 수행한다.
     */
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
