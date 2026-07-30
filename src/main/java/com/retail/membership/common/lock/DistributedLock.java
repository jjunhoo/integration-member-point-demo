package com.retail.membership.common.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * <p><b>용도:</b> 메서드에 Redisson 분산 락을 적용하기 위한 마커 어노테이션.</p>
 *
 * 분산 락 공통 어노테이션.
 *
 * <p>메서드에 부착하면 {@link DistributedLockAspect}가 대상 실행 전
 * Redisson 기반 분산 락을 획득하고, 실행 종료 후 반드시 해제한다.
 *
 * <p>락 키는 SpEL 표현식({@link #key()})으로 지정하며, 메서드 파라미터를
 * 참조할 수 있다. 예: {@code @DistributedLock(key = "'point:' + #userId")}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 락 키 SpEL 표현식. 유저 ID 또는 멤버십 카드 고유 키를 기반으로
     * 상호 배제(Mutual Exclusion) 단위를 정의한다.
     */
    String key();

    /** 락의 prefix 네임스페이스 (키 충돌 방지). */
    String prefix() default "membership:lock:";

    /** 락 획득을 기다리는 최대 시간. 초과 시 획득 실패로 간주한다. */
    long waitTime() default 3000L;

    /**
     * 락 임대(점유) 시간. 이 시간이 지나면 자동 해제되어
     * 락 홀더 장애 시 데드락을 방지한다.
     */
    long leaseTime() default 5000L;

    /** waitTime / leaseTime 의 시간 단위. */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}
