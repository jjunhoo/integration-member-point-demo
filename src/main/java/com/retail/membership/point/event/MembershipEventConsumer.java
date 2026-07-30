package com.retail.membership.point.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.membership.point.query.MembershipViewStore;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * <p><b>용도:</b> 도메인 이벤트를 소비해 Redis 조회 뷰를 동기화하는 Kafka 컨슈머.</p>
 *
 * 멤버십 도메인 이벤트 Consumer (CQRS 조회 뷰 동기화).
 *
 * <p>토픽 {@code membership.domain-event.v1} 를 구독하여 Redis 뷰의
 * 멤버십 등급/포인트 잔액을 준실시간으로 동기화한다.
 *
 * <h3>결함 허용(Fault Tolerance)</h3>
 * <ol>
 *   <li><b>Resilience4j @Retry</b>: 일시적 오류(네트워크 등)에 대해 메서드 내부에서
 *       지수 백오프로 재시도한다.</li>
 *   <li><b>Kafka DefaultErrorHandler + DLQ</b>: 컨테이너 레벨에서 재시도 후에도
 *       실패하면 {@code *.DLQ} 토픽(Dead Letter Queue)으로 자동 이관한다.
 *       (설정: {@code KafkaConsumerConfig})</li>
 * </ol>
 *
 * <p>수동 ack 를 사용하여, 처리가 성공적으로 끝난 뒤에만 오프셋을 커밋한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipEventConsumer {

    private final ObjectMapper objectMapper;
    private final MembershipViewStore viewStore;

    @Retry(name = "membership-view-sync")   // 일시 오류 시 메서드 단위 재시도
    @KafkaListener(
            topics = "${retail.membership.kafka.domain-event-topic}",
            groupId = "membership-view-sync",
            containerFactory = "membershipKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, String> record,
                        Acknowledgment acknowledgment,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            MembershipDomainEvent event =
                    objectMapper.readValue(record.value(), MembershipDomainEvent.class);

            log.debug("[Consumer] 수신 eventId={} userId={} type={} (p={}, offset={})",
                    event.eventId(), event.userId(), event.eventType(), partition, offset);

            // 핵심 처리: Redis 뷰 동기화 (멱등 upsert)
            viewStore.apply(event);

            // 처리 성공 → 수동 오프셋 커밋
            acknowledgment.acknowledge();

        } catch (Exception e) {
            // 예외를 다시 던져 컨테이너의 DefaultErrorHandler(재시도 → DLQ)로 위임한다.
            // (여기서 ack 하지 않으므로 오프셋은 커밋되지 않는다.)
            log.error("[Consumer] 처리 실패 → 에러 핸들러로 위임 offset={} value={}",
                    offset, record.value(), e);
            // 체크 예외를 언체크로 감싸 상위 에러 핸들러/재시도 로직으로 전달한다.
            throw new MembershipEventProcessingException("멤버십 이벤트 처리 실패 offset=" + offset, e);
        }
    }
}
