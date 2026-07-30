package com.retail.membership.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * {@link DistributedLock} 어노테이션을 처리하는 AOP Aspect.
 *
 * <h3>왜 트랜잭션보다 바깥에서 동작해야 하는가?</h3>
 * <p>{@code @Order}를 가장 높은 우선순위로 두어, 락 획득 → (트랜잭션 시작 →
 * 커밋) → 락 해제 순서를 보장한다. 만약 락이 트랜잭션 커밋 이전에 풀리면
 * 다른 스레드가 아직 커밋되지 않은(=이전 값이 보이는) 상태에서 로직을
 * 수행하여 레이스 컨디션이 재발할 수 있기 때문이다.
 */
@Slf4j
@Aspect
@Component
@Order(Integer.MIN_VALUE) // @Transactional(Ordered.LOWEST) 보다 항상 바깥에서 실행
/**
 * <p><b>용도:</b> @DistributedLock 메서드 전후로 Redis 락 획득/해제하는 AOP.</p>
 */
@RequiredArgsConstructor
public class DistributedLockAspect {

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    private final RedissonClient redissonClient;

    /** 최대 재시도 횟수 (락 경합 시). */
    private static final int MAX_RETRY = 2;
    /** 재시도 간 백오프 (ms). */
    private static final long RETRY_BACKOFF_MS = 150L;

    // 어노테이션을 advice 파라미터로 바인딩(@annotation(x))하면 일부 환경에서
    // "JoinPointMatch was NOT bound" 바인딩 오류가 발생한다. 따라서 마커 방식으로
    // 매칭만 하고, 어노테이션 값은 조인포인트에서 리플렉션으로 직접 추출한다.
    @Around("@annotation(com.retail.membership.common.lock.DistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        DistributedLock distributedLock =
                methodSignature.getMethod().getAnnotation(DistributedLock.class);

        // 1) SpEL 로 실제 락 키를 계산한다. (예: "point:12345")
        String lockKey = distributedLock.prefix() + resolveKey(joinPoint, distributedLock.key());
        RLock lock = redissonClient.getLock(lockKey);

        int attempt = 0;
        while (true) {
            attempt++;
            boolean acquired = false;
            try {
                // 2) tryLock: waitTime 동안 대기, leaseTime 후 자동 해제.
                //    leaseTime 을 명시하면 홀더 장애 시에도 락이 영구 점유되지 않는다.
                acquired = lock.tryLock(
                        distributedLock.waitTime(),
                        distributedLock.leaseTime(),
                        distributedLock.timeUnit());

                if (!acquired) {
                    // 3) 획득 실패 → 재시도 전략 적용
                    if (attempt <= MAX_RETRY) {
                        log.warn("[DistributedLock] 락 획득 실패, 재시도 {}/{} key={}", attempt, MAX_RETRY, lockKey);
                        Thread.sleep(RETRY_BACKOFF_MS * attempt);   // 선형 백오프
                        continue;
                    }
                    throw new LockAcquisitionException(
                            "분산 락 획득에 최종 실패했습니다. key=" + lockKey);
                }

                log.debug("[DistributedLock] 락 획득 성공 key={} (attempt={})", lockKey, attempt);

                // 4) 락을 점유한 상태에서 실제 비즈니스 로직(=트랜잭션)을 수행한다.
                return joinPoint.proceed();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockAcquisitionException("락 대기 중 인터럽트가 발생했습니다. key=" + lockKey, e);
            } finally {
                // 5) 현재 스레드가 점유한 락만 안전하게 해제한다.
                //    (자동 해제 이후일 수 있으므로 isHeldByCurrentThread 로 방어)
                if (acquired && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("[DistributedLock] 락 해제 완료 key={}", lockKey);
                }
            }
        }
    }

    /**
     * 메서드 파라미터를 참조하는 SpEL 표현식을 평가하여 락 키 문자열을 만든다.
     */
    private String resolveKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        Expression expression = SPEL_PARSER.parseExpression(keyExpression);
        Object value = expression.getValue(context);
        return String.valueOf(value);
    }
}
