package com.retail.membership.point.event;

import java.time.Instant;
import java.util.UUID;

import com.retail.membership.point.domain.MembershipGrade;

/**
 * <p><b>용도:</b> Kafka 로 전파되는 멤버십 포인트 도메인 이벤트 페이로드.</p>
 *
 * <p>변경 후 스냅샷을 실어 Query 측이 Master 조회 없이 Redis 뷰를 upsert 한다.
 */
public record MembershipDomainEvent(
        /** 멱등 처리용 고유 이벤트 ID. */
        String eventId,
        /** 유저 ID (Kafka 파티션 키). */
        String userId,
        /** 이벤트 유형 (적립/차감/만료 등). */
        MembershipEventType eventType,
        /** 이번 변동 금액. */
        long amount,
        /** 변경 후 포인트 잔액. */
        long pointBalance,
        /** 변경 후 누적 적립 포인트. */
        long totalAccumulatedPoint,
        /** 변경 후 등급. */
        MembershipGrade grade,
        /** 이벤트 발생 시각. */
        Instant occurredAt
) {
    /**
     * 도메인 이벤트를 생성한다.
     * eventId 는 UUID, occurredAt 은 현재 시각으로 자동 설정한다.
     */
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
