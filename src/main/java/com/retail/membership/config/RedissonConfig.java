package com.retail.membership.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p><b>용도:</b> 분산 락·Redis 뷰에 쓰는 RedissonClient 빈 구성.</p>
 *
 * Redisson 클라이언트 설정.
 *
 * <p>분산 락(RLock)과 Cache-Aside 뷰(RBucket)를 모두 이 클라이언트로 사용한다.
 * 뷰 객체(레코드) 직렬화를 위해 JSON 코덱을 사용한다.
 */
@Configuration
public class RedissonConfig {

    @Value("${redisson.address}")
    private String address;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        // Redisson 코덱용 ObjectMapper. JavaTimeModule 을 등록해야
        // MembershipView 의 Instant(java.time) 필드를 직렬화할 수 있다.
        ObjectMapper redissonMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Config config = new Config();
        config.setCodec(new JsonJacksonCodec(redissonMapper));   // MembershipView 등 POJO 직렬화
        config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(8)
                .setConnectionPoolSize(32);
        // 운영에서는 Sentinel/Cluster 모드로 전환하여 락의 가용성을 높인다.
        return Redisson.create(config);
    }
}
