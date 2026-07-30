package com.retail.membership.point.query;

import java.io.Serializable;
import java.time.Instant;

import com.retail.membership.point.domain.MembershipGrade;

/**
 * <p><b>용도:</b> CQRS 조회 측 Redis 에 저장되는 잔액/등급 스냅샷 모델.</p>
 */
public record MembershipView(
        /** 유저(멤버십) 식별자. */
        String userId,
        /** 사용 가능 포인트 잔액. */
        long pointBalance,
        /** 멤버십 등급. */
        MembershipGrade grade,
        /** 마지막으로 반영한 도메인 이벤트 ID (멱등). */
        String lastEventId,
        /** 뷰가 Redis 에 동기화된 시각. */
        Instant syncedAt
) implements Serializable {
}
