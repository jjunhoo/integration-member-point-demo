package com.retail.membership.auth.jwt;

/**
 * <p><b>용도:</b> 발급된 accessToken/refreshToken 쌍을 담는 값 객체.</p>
 */
public record TokenPair(
        /** API 인증용 단기 access JWT. */
        String accessToken,
        /** 재발급용 장기 refresh JWT. */
        String refreshToken,
        /** access 토큰 만료까지 남은 초. */
        long accessTokenExpiresIn
) {
}
