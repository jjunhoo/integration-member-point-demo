package com.retail.membership.point.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.membership.point.domain.Membership;
import com.retail.membership.point.domain.MembershipRepository;
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
 * <p><b>용도:</b> MembershipView 를 Redis 에 읽고 이벤트 기준으로 upsert 하는 저장소.</p>
 *
 * Redis 기반 멤버십 뷰 저장소 (Cache-Aside 조회 모델).
 *
 * <p>Command 측 이벤트를 구독하여 준실시간으로 등급/잔액을 갱신하며,
 * 조회 API 는 Redis 를 먼저 읽고(캐시 히트), 미스 시 Master DB 로 폴백한 뒤
 * Redis 에 재적재하는 Cache-Aside 전략을 사용한다.
 *
 * <p>직렬화는 Redisson 기본 코덱의 타입 태깅(@class)에 의존하지 않고,
 * {@link StringCodec} + 애플리케이션 {@link ObjectMapper} 로 JSON 문자열을
 * 직접 다룬다. 저장 포맷을 명확히 통제하기 위함이다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MembershipViewStore {

    /** Redis 키 prefix. 실제 키는 {@code membership:view:{userId}}. */
    private static final String KEY_PREFIX = "membership:view:";

    /** 뷰 TTL. 갱신되지 않으면 만료되어 Cache-Aside 미스로 이어질 수 있다. */
    private static final Duration TTL = Duration.ofDays(1);

    /** RDB 폴백으로 재적재할 때 넣는 표식 eventId (Kafka 이벤트와 구분). */
    private static final String CACHE_ASIDE_EVENT_ID = "cache-aside";

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final MembershipRepository membershipRepository;

    /**
     * userId 로 조회 뷰를 반환한다 (Cache-Aside).
     *
     * <ol>
     *   <li>Redis 히트면 그대로 반환</li>
     *   <li>미스면 Master DB({@code membership}) 조회 후 Redis 에 재적재하고 반환</li>
     *   <li>DB에도 없으면 empty</li>
     * </ol>
     */
    public Optional<MembershipView> find(String userId) {
        RBucket<String> bucket = redissonClient.getBucket(key(userId), StringCodec.INSTANCE);
        String json = bucket.get();
        if (json != null) {
            return Optional.of(readValue(json));
        }
        return loadFromDbAndCache(userId, bucket);
    }

    /**
     * 도메인 이벤트 스냅샷으로 뷰를 upsert 한다.
     *
     * <p>이벤트에 실린 잔액/등급으로 Redis 값을 덮어쓴다.
     * 이미 동일 {@code eventId} 가 반영된 경우 건너뛰어 Kafka at-least-once 중복을 흡수한다.
     *
     * @param event 커밋 후 발행된 멤버십 도메인 이벤트
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
        put(bucket, updated);
        log.debug("[View] 뷰 동기화 완료 userId={} balance={} grade={}",
                event.userId(), event.pointBalance(), event.grade());
    }

    /** Redis 미스 시 Master DB 를 읽고 캐시에 다시 심는다. */
    private Optional<MembershipView> loadFromDbAndCache(String userId, RBucket<String> bucket) {
        Optional<Membership> membership = membershipRepository.findById(userId);
        if (membership.isEmpty()) {
            log.debug("[View] Cache-Aside 미스 + DB 없음 userId={}", userId);
            return Optional.empty();
        }

        Membership m = membership.get();
        MembershipView view = new MembershipView(
                m.getUserId(),
                m.getPointBalance(),
                m.getGrade(),
                CACHE_ASIDE_EVENT_ID,
                Instant.now());
        put(bucket, view);
        log.info("[View] Cache-Aside 재적재 userId={} balance={} grade={}",
                m.getUserId(), m.getPointBalance(), m.getGrade());
        return Optional.of(view);
    }

    /** 뷰를 TTL 과 함께 Redis 에 저장한다. */
    private void put(RBucket<String> bucket, MembershipView view) {
        bucket.set(writeValue(view), TTL);
    }

    /** Redis 버킷 키를 만든다. */
    private String key(String userId) {
        return KEY_PREFIX + userId;
    }

    /** JSON 문자열을 {@link MembershipView} 로 역직렬화한다. */
    private MembershipView readValue(String json) {
        try {
            return objectMapper.readValue(json, MembershipView.class);
        } catch (Exception e) {
            throw new IllegalStateException("멤버십 뷰 역직렬화 실패", e);
        }
    }

    /** {@link MembershipView} 를 JSON 문자열로 직렬화한다. */
    private String writeValue(MembershipView view) {
        try {
            return objectMapper.writeValueAsString(view);
        } catch (Exception e) {
            throw new IllegalStateException("멤버십 뷰 직렬화 실패", e);
        }
    }
}
