package com.retail.membership.auth.jwt;

/**
 * 발급된 access/refresh 토큰 쌍.
 *
 * @param accessToken            API 인증용 단기 토큰
 * @param refreshToken           재발급용 장기 토큰
 * @param accessTokenExpiresIn   access 토큰 만료(초)
 */
public record TokenPair(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn
) {
}
