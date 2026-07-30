package com.retail.membership.point.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * <p><b>용도:</b> 트랜잭션 커밋 후 스프링 이벤트를 Kafka 발행으로 연결하는 브릿지.</p>
 *
 * 트랜잭션 커밋 이후 도메인 이벤트를 Kafka 로 전달하는 브릿지.
 *
 * <p>{@code @TransactionalEventListener(phase = AFTER_COMMIT)} 를 사용하여
 * <b>Master DB 트랜잭션이 정상 커밋된 뒤에만</b> 리스너가 호출된다.
 * 트랜잭션이 롤백되면 이 리스너는 실행되지 않으므로, DB 변경과 Kafka 발행의
 * 정합성(유령 이벤트 방지)이 보장된다.
 *
 * <p>참고: AFTER_COMMIT 시점은 이미 트랜잭션이 종료된 상태다. 따라서 이 안에서
 * 다시 DB 를 쓰려면 새 트랜잭션(REQUIRES_NEW)이 필요하다. 여기서는 Kafka 발행만
 * 수행하므로 별도 트랜잭션이 필요 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipEventPublisher {

    private final MembershipEventProducer eventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(MembershipDomainEvent event) {
        log.debug("[AFTER_COMMIT] 커밋 확인 → Kafka 발행 트리거 eventId={} userId={}",
                event.eventId(), event.userId());
        eventProducer.publish(event);
    }
}
