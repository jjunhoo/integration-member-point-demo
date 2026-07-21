package com.retail.membership.auth.api;

import com.retail.membership.auth.jwt.TokenPair;

/** 토큰 발급 응답. */
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
