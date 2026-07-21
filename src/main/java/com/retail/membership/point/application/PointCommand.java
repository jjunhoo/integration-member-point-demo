package com.retail.membership.point.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 포인트 적립/차감 커맨드 DTO.
 *
 * @param userId 유저(멤버십 카드) 식별자
 * @param amount 변동 금액 (양수)
 */
public record PointCommand(
        @NotBlank String userId,
        @Positive long amount
) {
}
