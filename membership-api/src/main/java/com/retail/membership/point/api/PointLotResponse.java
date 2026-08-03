package com.retail.membership.point.api;

import com.retail.membership.point.domain.PointLot;

import java.time.Instant;

/**
 * <p><b>용도:</b> 포인트 lot 잔여/만료 정보를 내려주는 조회 응답 DTO.</p>
 */
public record PointLotResponse(
        /** lot PK. */
        Long id,
        /** 유저(멤버십) ID. */
        String userId,
        /** 최초 적립 금액. */
        long originalAmount,
        /** 잔여 금액. */
        long remainingAmount,
        /** 적립 시각. */
        Instant earnedAt,
        /** 만료 시각. */
        Instant expiresAt,
        /** 조회 시점 기준 사용 가능 여부. */
        boolean usable
) {
    /**
     * PointLot 엔티티를 조회 응답 DTO 로 변환한다.
     * {@code now} 기준 사용 가능 여부를 함께 계산한다.
     */
    public static PointLotResponse from(PointLot lot, Instant now) {
        return new PointLotResponse(
                lot.getId(),
                lot.getUserId(),
                lot.getOriginalAmount(),
                lot.getRemainingAmount(),
                lot.getEarnedAt(),
                lot.getExpiresAt(),
                lot.isUsableAt(now));
    }
}
