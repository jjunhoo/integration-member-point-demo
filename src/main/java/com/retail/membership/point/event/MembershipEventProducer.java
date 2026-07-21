package com.retail.membership.point.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 멤버십 도메인 이벤트 Kafka 발행자.
 *
 * <p>토픽 {@code membership.domain-event.v1} 로 발행한다.
 * 파티션 키를 userId 로 지정하여 동일 유저 이벤트의 순서를 보장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${retail.membership.kafka.domain-event-topic}")
    private String topic;

    /**
     * 도메인 이벤트를 발행한다. 프로듀서는 멱등성(enable.idempotence)과
     * acks=all 로 설정되어 있어 at-least-once 를 보장한다.
     */
    public void publish(MembershipDomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            // 파티션 키 = userId → 같은 유저의 이벤트는 같은 파티션 → 순서 보장
            CompletableFuture<?> future = kafkaTemplate.send(topic, event.userId(), payload);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    // 커밋 이후 발행이므로 여기서의 실패는 "이미 확정된 변경에 대한 이벤트 유실"이다.
                    // 운영에서는 Transactional Outbox 패턴으로 유실을 원천 차단하는 것을 권장한다.
                    log.error("[Producer] Kafka 발행 실패 eventId={} userId={}",
                            event.eventId(), event.userId(), ex);
                } else {
                    log.debug("[Producer] Kafka 발행 성공 eventId={} userId={} type={}",
                            event.eventId(), event.userId(), event.eventType());
                }
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("도메인 이벤트 직렬화 실패 eventId=" + event.eventId(), e);
        }
    }
}
