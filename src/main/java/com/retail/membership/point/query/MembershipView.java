package com.retail.membership.point.query;

import java.io.Serializable;
import java.time.Instant;

import com.retail.membership.point.domain.MembershipGrade;

/**
 * 읽기 전용 멤버십 뷰 (Redis Cache-Aside 저장 모델, CQRS의 조회 측).
 *
 * @param userId       유저 식별자
 * @param pointBalance 포인트 잔액
 * @param grade        멤버십 등급
 * @param lastEventId  마지막으로 반영한 이벤트 ID (멱등/중복 방지용)
 * @param syncedAt     뷰 동기화 시각
 */
public record MembershipView(
        String userId,
        long pointBalance,
        MembershipGrade grade,
        String lastEventId,
        Instant syncedAt
) implements Serializable {
}
