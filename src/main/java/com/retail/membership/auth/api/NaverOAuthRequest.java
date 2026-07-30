package com.retail.membership.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * <p><b>용도:</b> 네이버 콜백 code/state 로 로그인할 때 쓰는 요청 DTO.</p>
 */
public record NaverOAuthRequest(
        /** 네이버 인가 코드. */
        @NotBlank String code,
        /** CSRF 방지용 state. */
        @NotBlank String state
) {
}
