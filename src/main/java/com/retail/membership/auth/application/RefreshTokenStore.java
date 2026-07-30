package com.retail.membership.auth.application;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * <p><b>용도:</b> 회원별 refresh 토큰을 Redis 에 저장·대조·회수하는 저장소.</p>
 *
 * Refresh 토큰 저장소 (Redis).
 *
 * <p>회원별 최신 refresh 토큰을 저장하여 (1) 토큰 재발급 시 유효성 대조,
 * (2) 로그아웃/강제 만료(회수)를 가능하게 한다. Stateless access 토큰의 약점인
 * "즉시 무효화 불가"를 refresh 회수로 보완한다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final RedissonClient redissonClient;

    public void save(String memberId, String refreshToken, long ttlSeconds) {
        bucket(memberId).set(refreshToken, Duration.ofSeconds(ttlSeconds));
    }

    /** 저장된 토큰과 일치하는지 확인 (재발급 시 사용). */
    public boolean matches(String memberId, String refreshToken) {
        String stored = bucket(memberId).get();
        return stored != null && stored.equals(refreshToken);
    }

    public void revoke(String memberId) {
        bucket(memberId).delete();
    }

    private RBucket<String> bucket(String memberId) {
        return redissonClient.getBucket(KEY_PREFIX + memberId, StringCodec.INSTANCE);
    }
}
