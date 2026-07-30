package com.retail.membership.auth.jwt;

/**
 * <p><b>용도:</b> 발급된 accessToken/refreshToken 쌍을 담는 값 객체.</p>
 *
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
