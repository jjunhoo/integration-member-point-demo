package com.retail.membership.point.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * <p><b>용도:</b> 처리 실패 이벤트를 DLQ 에서 모니터링/로깅하는 Kafka 컨슈머.</p>
 *
 * Dead Letter Queue(DLQ) Consumer.
 *
 * <p>정상 Consumer 가 재시도까지 소진하고도 처리에 실패한 메시지는
 * {@code membership.domain-event.v1.DLQ} 토픽으로 이관된다.
 * 여기서는 실패 메시지를 기록/알림하여 운영자가 수동 개입하거나
 * 재처리 배치를 돌릴 수 있도록 한다.
 */
@Slf4j
@Component
public class MembershipDlqConsumer {

    @KafkaListener(
            topics = "${retail.membership.kafka.dlq-topic}",
            groupId = "membership-dlq-monitor",
            containerFactory = "membershipKafkaListenerContainerFactory")
    public void consumeDlq(ConsumerRecord<String, String> record,
                           Acknowledgment acknowledgment,
                           @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String exceptionMessage,
                           @Header(name = KafkaHeaders.DLT_ORIGINAL_TOPIC, required = false) String originalTopic) {
        // 실무에서는 여기서 Sentry/Slack 알림, 실패 로그 테이블 적재, 재처리 큐 등록 등을 수행한다.
        log.error("[DLQ] 최종 실패 메시지 수신. originalTopic={} cause={} payload={}",
                originalTopic, exceptionMessage, record.value());

        // DLQ 는 모니터링/보관이 목적이므로 항상 커밋하여 무한 루프를 방지한다.
        acknowledgment.acknowledge();
    }
}
