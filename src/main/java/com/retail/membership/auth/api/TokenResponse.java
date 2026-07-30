package com.retail.membership.auth.api;

import com.retail.membership.auth.jwt.TokenPair;

/**
 * <p><b>용도:</b> 로그인/가입/재발급 결과로 access·refresh 토큰을 반환하는 응답 DTO.</p>
 */
public record TokenResponse(
        /** API 인증용 access JWT. */
        String accessToken,
        /** 재발급용 refresh JWT. */
        String refreshToken,
        /** 토큰 타입 (항상 Bearer). */
        String tokenType,
        /** access 토큰 만료까지 남은 초. */
        long expiresIn
) {
    /** TokenPair를 API 응답 DTO로 변환한다. */
    public static TokenResponse from(TokenPair pair) {
        return new TokenResponse(
                pair.accessToken(),
                pair.refreshToken(),
                "Bearer",
                pair.accessTokenExpiresIn());
    }
}
