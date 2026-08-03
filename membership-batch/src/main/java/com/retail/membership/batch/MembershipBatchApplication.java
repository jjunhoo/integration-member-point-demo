package com.retail.membership.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 포인트 만료 등 배치 Job 전용 애플리케이션.
 *
 * <p>도메인·커맨드·이벤트 발행은 {@code membership-common} 을 재사용하고,
 * API 모듈(웹/시큐리티/컨슈머)과는 프로세스를 분리한다.
 */
@SpringBootApplication(scanBasePackages = "com.retail.membership")
@EntityScan(basePackages = "com.retail.membership")
@EnableJpaRepositories(basePackages = "com.retail.membership")
public class MembershipBatchApplication {

    public static void main(String[] args) {
        // Job 종료 후에도 Redisson/Kafka/Hikari non-daemon 스레드가 JVM 을 붙잡으므로
        // 컨텍스트를 닫고 프로세스 exit code 로 종료한다. (배치 앱 권장 패턴)
        System.exit(SpringApplication.exit(
                SpringApplication.run(MembershipBatchApplication.class, args)));
    }
}

