package com.retail.membership.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * <p><b>용도:</b> Kafka 리스너 컨테이너·재시도·DLQ 에러 핸들러 설정.</p>
 *
 * Kafka Consumer 설정 + 결함 허용(재시도/DLQ) 설정.
 *
 * <h3>에러 핸들링 전략</h3>
 * <ul>
 *   <li><b>DefaultErrorHandler</b>: 리스너에서 예외가 전파되면 지수 백오프로 재시도.</li>
 *   <li><b>DeadLetterPublishingRecoverer</b>: 재시도 소진 시 Dead Letter Queue(토픽)로
 *       실패 메시지를 발행. Spring Kafka 문서 용어는 DLT 이지만, 본 프로젝트는 DLQ 로 통일.</li>
 * </ul>
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${retail.membership.kafka.dlq-topic}")
    private String dlqTopic;

    /** 수동 커밋·문자열 역직렬화를 사용하는 Kafka ConsumerFactory를 생성한다. */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);   // 수동 커밋
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * 리스너 컨테이너 팩토리. 수동 ack 모드 + 에러 핸들러(재시도/DLQ)를 장착한다.
     *
     * @param kafkaTemplate DLQ 로 실패 메시지를 발행하기 위한 템플릿(자동 구성 재사용)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> membershipKafkaListenerContainerFactory(
            KafkaTemplate<String, String> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);   // 파티션 병렬 소비
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        factory.setCommonErrorHandler(errorHandler(kafkaTemplate));
        return factory;
    }

    /**
     * 재시도 후 DLQ 로 이관하는 에러 핸들러.
     * 지수 백오프: 초기 500ms → 배수 2.0 → 최대 5초, 최대 대략 3회 재시도.
     */
    private DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        // 실패한 레코드를 DLQ 토픽(membership.domain-event.v1.DLQ)으로 라우팅한다.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new org.apache.kafka.common.TopicPartition(dlqTopic, record.partition()));

        ExponentialBackOff backOff = new ExponentialBackOff(500L, 2.0);
        backOff.setMaxElapsedTime(10_000L);   // 총 재시도 시간 상한

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // 역직렬화 실패 등 재시도가 무의미한 예외는 즉시 DLQ 로 보낸다.
        handler.addNotRetryableExceptions(
                com.fasterxml.jackson.core.JsonProcessingException.class,
                IllegalArgumentException.class);
        return handler;
    }
}
