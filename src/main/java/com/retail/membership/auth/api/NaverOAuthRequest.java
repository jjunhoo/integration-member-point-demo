package com.retail.membership.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * 네이버 인가 코드 로그인 요청.
 *
 * @param code  네이버가 콜백으로 내려준 authorization code
 * @param state CSRF 방지용 state (authorize-url 발급 시 저장한 값)
 */
public record NaverOAuthRequest(
        @NotBlank String code,
        @NotBlank String state
) {
}
