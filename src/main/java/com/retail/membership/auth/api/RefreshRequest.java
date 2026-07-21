package com.retail.membership.auth.api;

import jakarta.validation.constraints.NotBlank;

/** 토큰 재발급 요청. */
public record RefreshRequest(
        @NotBlank String refreshToken
) {
}
