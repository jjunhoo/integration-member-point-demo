package com.retail.membership.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * <p><b>용도:</b> refreshToken 으로 access/refresh 를 재발급할 때 쓰는 요청 DTO.</p>
 *
 * 토큰 재발급 요청.
 */
public record RefreshRequest(
        @NotBlank String refreshToken
) {
}
