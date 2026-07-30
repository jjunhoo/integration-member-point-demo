package com.retail.membership.point.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

/**
 * <p><b>용도:</b> 포인트 적립/차감 API·서비스에 넘기는 커맨드 DTO.</p>
 */
public record PointCommand(
        /** 유저(멤버십) 식별자. */
        @NotBlank String userId,
        /** 변동 금액 (양수). */
        @Positive long amount,
        /** 적립 만료 시각(선택). 차감 시 무시. null 이면 적립+1년. */
        Instant expiresAt
) {
}
