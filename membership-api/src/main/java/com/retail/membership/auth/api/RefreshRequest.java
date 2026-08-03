package com.retail.membership.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * <p><b>용도:</b> refreshToken 으로 access/refresh 를 재발급할 때 쓰는 요청 DTO.</p>
 */
public record RefreshRequest(
        /** 재발급에 사용할 refresh JWT. */
        @NotBlank String refreshToken
) {
}
