package com.retail.membership.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * <p><b>용도:</b> 데모용 Kafka 토픽(도메인 이벤트/DLQ) 자동 생성 설정.</p>
 *
 * 데모용 토픽 자동 생성. (운영에서는 인프라(IaC)로 사전 프로비저닝하는 것이 일반적)
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${retail.membership.kafka.domain-event-topic}")
    private String domainEventTopic;

    @Value("${retail.membership.kafka.dlq-topic}")
    private String dlqTopic;

    @Bean
    public NewTopic domainEventTopic() {
        // 동일 유저 순서 보장을 위해 파티션 키(userId)를 사용하므로 파티션 수는 처리량에 맞춰 설정
        return TopicBuilder.name(domainEventTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(dlqTopic).partitions(3).replicas(1).build();
    }
}
