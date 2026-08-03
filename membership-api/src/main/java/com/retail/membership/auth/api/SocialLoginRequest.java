package com.retail.membership.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * <p><b>용도:</b> 이미 발급된 소셜 provider 토큰으로 직접 로그인할 때 쓰는 요청 DTO.</p>
 */
public record SocialLoginRequest(
        /** provider access token(kakao/naver) 또는 id_token(apple). */
        @NotBlank String token
) {
}
