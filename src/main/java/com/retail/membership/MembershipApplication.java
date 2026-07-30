package com.retail.membership;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;

/**
 * <p><b>용도:</b> Spring Boot 진입점. 통합 회원·인증·포인트(CQRS/Kafka) 애플리케이션을 기동한다.</p>
 *
 * 4대 채널(편의점 / 슈퍼 / 홈쇼핑 / O4O) 통합 회원·멤버십 시스템.
 * 동시성 제어 + 비동기 이벤트 전파 + 소셜 로그인(JWT) 레이어.
 *
 * <p>CQRS 지향:
 * <ul>
 *   <li>Command : Master DB(RDB)에 대한 멤버십/포인트 변경 + 분산 락으로 동시성 제어</li>
 *   <li>Query   : Kafka로 전파된 도메인 이벤트를 구독하여 Redis 뷰(Cache-Aside)를 준실시간 동기화</li>
 * </ul>
 */
@EnableKafka
@EnableRetry
@SpringBootApplication
public class MembershipApplication {

    /** Spring Boot 애플리케이션을 기동한다. */
    public static void main(String[] args) {
        SpringApplication.run(MembershipApplication.class, args);
    }
}
