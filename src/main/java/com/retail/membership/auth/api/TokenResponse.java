package com.retail.membership.auth.api;

import com.retail.membership.auth.jwt.TokenPair;

/**
 * <p><b>용도:</b> 로그인/가입/재발급 결과로 access·refresh 토큰을 반환하는 응답 DTO.</p>
 *
 * 토큰 발급 응답.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static TokenResponse from(TokenPair pair) {
        return new TokenResponse(
                pair.accessToken(),
                pair.refreshToken(),
                "Bearer",
                pair.accessTokenExpiresIn());
    }
}
