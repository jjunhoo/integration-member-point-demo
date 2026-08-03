package com.retail.membership.auth.social;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * <p><b>용도:</b> OAuth CSRF 방지용 state 를 Redis 에 발급·1회 소비하는 저장소.</p>
 *
 * OAuth state 저장소 (Redis). CSRF 방지용 일회성 값.
 */
@Component
@RequiredArgsConstructor
public class OAuthStateStore {

    private static final String KEY_PREFIX = "auth:oauth:state:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedissonClient redissonClient;

    /** state 를 발급하고 저장한다. */
    public String issue(String provider) {
        String state = UUID.randomUUID().toString().replace("-", "");
        bucket(state).set(provider, TTL);
        return state;
    }

    /**
     * state 가 유효하면 소비(삭제)하고 true.
     * 재사용/만료면 false.
     */
    public boolean consume(String state, String expectedProvider) {
        if (state == null || state.isBlank()) {
            return false;
        }
        RBucket<String> bucket = bucket(state);
        String provider = bucket.get();
        if (provider == null || !provider.equalsIgnoreCase(expectedProvider)) {
            return false;
        }
        bucket.delete();
        return true;
    }

    /** state 값 기준 Redis 버킷을 반환한다. */
    private RBucket<String> bucket(String state) {
        return redissonClient.getBucket(KEY_PREFIX + state, StringCodec.INSTANCE);
    }
}
