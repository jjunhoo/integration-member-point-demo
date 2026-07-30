package com.retail.membership.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * 소셜 로그인 요청 (이미 발급된 provider 토큰을 직접 전달하는 경우).
 *
 * @param token access token(kakao/naver) 또는 id_token(apple)
 */
public record SocialLoginRequest(
        @NotBlank String token
) {
}
