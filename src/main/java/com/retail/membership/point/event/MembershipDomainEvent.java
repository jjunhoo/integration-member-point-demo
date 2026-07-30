package com.retail.membership.point.event;

import java.time.Instant;
import java.util.UUID;

import com.retail.membership.point.domain.MembershipGrade;

/**
 * <p><b>용도:</b> Kafka 로 전파되는 멤버십 포인트 도메인 이벤트 페이로드.</p>
 *
 * 멤버십 도메인 이벤트 (Kafka 페이로드).
 *
 * <p>토픽 {@code membership.domain-event.v1} 로 발행되며, Query 측
 * Consumer 가 이를 구독하여 Redis 뷰(등급/잔액)를 동기화한다.
 *
 * <p>변경 이후의 최종 스냅샷(잔액/누적/등급)을 함께 실어, Consumer 가 조회 없이
 * 뷰를 갱신(idempotent upsert)할 수 있도록 한다.
 *
 * @param eventId        멱등 처리를 위한 고유 이벤트 ID
 * @param userId         유저(멤버십 카드) 식별자 → Kafka 파티션 키로도 사용
 * @param eventType      이벤트 유형
 * @param amount         변동 금액(적립/차감량)
 * @param pointBalance   변경 후 포인트 잔액
 * @param totalAccumulatedPoint 변경 후 누적 적립 포인트
 * @param grade          변경 후 등급
 * @param occurredAt     이벤트 발생 시각
 */
public record MembershipDomainEvent(
        String eventId,
        String userId,
        MembershipEventType eventType,
        long amount,
        long pointBalance,
        long totalAccumulatedPoint,
        MembershipGrade grade,
        Instant occurredAt
) {
    public static MembershipDomainEvent of(
            String userId,
            MembershipEventType eventType,
            long amount,
            long pointBalance,
            long totalAccumulatedPoint,
            MembershipGrade grade) {
        return new MembershipDomainEvent(
                UUID.randomUUID().toString(),
                userId,
                eventType,
                amount,
                pointBalance,
                totalAccumulatedPoint,
                grade,
                Instant.now());
    }
}
