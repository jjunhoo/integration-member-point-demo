package com.retail.membership.point.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.membership.point.event.MembershipDomainEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Redis 기반 멤버십 뷰 저장소 (Cache-Aside 조회 모델).
 *
 * <p>Command 측 이벤트를 구독하여 준실시간으로 등급/잔액을 갱신하며,
 * 조회 API 는 이 저장소를 먼저 읽고(캐시 히트), 미스 시 Master DB 로 폴백하는
 * Cache-Aside 전략을 사용한다.
 *
 * <p>직렬화는 Redisson 기본 코덱의 타입 태깅(@class)에 의존하지 않고,
 * {@link StringCodec} + 애플리케이션 {@link ObjectMapper} 로 JSON 문자열을
 * 직접 다룬다. 저장 포맷을 명확히 통제하기 위함이다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MembershipViewStore {

    private static final String KEY_PREFIX = "membership:view:";
    private static final Duration TTL = Duration.ofDays(1);

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public Optional<MembershipView> find(String userId) {
        RBucket<String> bucket = redissonClient.getBucket(key(userId), StringCodec.INSTANCE);
        String json = bucket.get();
        if (json == null) {
            return Optional.empty();
        }
        return Optional.of(readValue(json));
    }

    /**
     * 도메인 이벤트를 뷰에 반영(upsert)한다.
     *
     * <p>멱등성 보장: 이미 동일 {@code eventId} 를 반영했다면 재적용하지 않는다.
     * (Kafka at-least-once 로 인한 중복 수신에 대비)
     */
    public void apply(MembershipDomainEvent event) {
        RBucket<String> bucket = redissonClient.getBucket(key(event.userId()), StringCodec.INSTANCE);
        String currentJson = bucket.get();

        if (currentJson != null) {
            MembershipView current = readValue(currentJson);
            if (event.eventId().equals(current.lastEventId())) {
                log.debug("[View] 중복 이벤트 스킵 eventId={} userId={}", event.eventId(), event.userId());
                return;
            }
        }

        MembershipView updated = new MembershipView(
                event.userId(),
                event.pointBalance(),
                event.grade(),
                event.eventId(),
                Instant.now());

        // 이벤트가 최종 스냅샷을 담고 있으므로 단순 덮어쓰기로 준실시간 동기화가 가능하다.
        bucket.set(writeValue(updated), TTL);
        log.debug("[View] 뷰 동기화 완료 userId={} balance={} grade={}",
                event.userId(), event.pointBalance(), event.grade());
    }

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }

    private MembershipView readValue(String json) {
        try {
            return objectMapper.readValue(json, MembershipView.class);
        } catch (Exception e) {
            throw new IllegalStateException("멤버십 뷰 역직렬화 실패", e);
        }
    }

    private String writeValue(MembershipView view) {
        try {
            return objectMapper.writeValueAsString(view);
        } catch (Exception e) {
            throw new IllegalStateException("멤버십 뷰 직렬화 실패", e);
        }
    }
}
