package com.retail.membership.point.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.retail.membership.common.lock.DistributedLock;
import com.retail.membership.point.domain.Membership;
import com.retail.membership.point.domain.MembershipRepository;
import com.retail.membership.point.event.MembershipDomainEvent;
import com.retail.membership.point.event.MembershipEventType;

import java.util.concurrent.TimeUnit;

/**
 * 멤버십 Command 서비스 (CQRS의 쓰기 측).
 *
 * <h3>실행 순서 (동시성 + 트랜잭션 + 이벤트)</h3>
 * <pre>
 *   [분산 락 획득]  ← @DistributedLock (AOP, 트랜잭션보다 바깥)
 *      └─ [트랜잭션 시작] ← @Transactional
 *            └─ Master DB 포인트/등급 변경
 *            └─ ApplicationEventPublisher 로 도메인 이벤트 "예약"
 *      └─ [트랜잭션 커밋]
 *            └─ @TransactionalEventListener(AFTER_COMMIT) 가 Kafka 발행 트리거
 *   [분산 락 해제]
 * </pre>
 *
 * <p>핵심: Kafka 발행은 <b>DB 커밋이 성공한 이후에만</b> 일어난다. 커밋 전에
 * 발행하면 트랜잭션이 롤백됐을 때 존재하지 않는 변경에 대한 이벤트가 나가는
 * "유령 이벤트" 문제가 생기기 때문이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipCommandService {

    private final MembershipRepository membershipRepository;
    // 스프링 내부 이벤트 발행기. 커밋 이후 Kafka 발행을 트리거하기 위한 브릿지.
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 포인트 적립.
     *
     * <p>{@code @DistributedLock} 으로 동일 유저의 동시 요청을 직렬화한다.
     * 락 키는 SpEL 로 {@code command.userId} 를 참조한다.
     */
    @DistributedLock(
            key = "'point:' + #command.userId()",
            waitTime = 3000L,
            leaseTime = 5000L,
            timeUnit = TimeUnit.MILLISECONDS)
    @Transactional
    public void accumulatePoint(PointCommand command) {
        Membership membership = getOrCreate(command.userId());

        membership.accumulate(command.amount());
        log.debug("[Command] 적립 완료 userId={} amount={} balance={} grade={}",
                command.userId(), command.amount(), membership.getPointBalance(), membership.getGrade());

        // 트랜잭션 커밋 이후 발행할 도메인 이벤트를 "예약"한다.
        publishAfterCommit(membership, MembershipEventType.POINT_ACCUMULATED, command.amount());
    }

    /**
     * 포인트 차감(사용).
     */
    @DistributedLock(
            key = "'point:' + #command.userId()",
            waitTime = 3000L,
            leaseTime = 5000L,
            timeUnit = TimeUnit.MILLISECONDS)
    @Transactional
    public void deductPoint(PointCommand command) {
        Membership membership = membershipRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 멤버십입니다. userId=" + command.userId()));

        membership.deduct(command.amount());
        log.debug("[Command] 차감 완료 userId={} amount={} balance={}",
                command.userId(), command.amount(), membership.getPointBalance());

        publishAfterCommit(membership, MembershipEventType.POINT_DEDUCTED, command.amount());
    }

    private Membership getOrCreate(String userId) {
        return membershipRepository.findById(userId)
                .orElseGet(() -> membershipRepository.save(new Membership(userId)));
    }

    /**
     * 변경 후 스냅샷을 담은 도메인 이벤트를 스프링 이벤트로 발행한다.
     * 실제 Kafka 전송은 커밋 이후 {@code MembershipEventPublisher} 가 담당한다.
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
