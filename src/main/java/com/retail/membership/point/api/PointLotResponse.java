package com.retail.membership.point.api;

import com.retail.membership.point.domain.PointLot;

import java.time.Instant;

/**
 * 포인트 lot 조회 응답.
 */
public record PointLotResponse(
        Long id,
        String userId,
        long originalAmount,
        long remainingAmount,
        Instant earnedAt,
        Instant expiresAt,
        boolean usable
) {
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
