package com.retail.membership.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p><b>용도:</b> JWT 서명 키·만료시간 등 application.yml 바인딩 설정.</p>
 */
@ConfigurationProperties(prefix = "retail.auth.jwt")
public record JwtProperties(
        /** HS256 서명용 시크릿 (256bit 이상 권장). */
        String secret,
        /** JWT iss 클레임. */
        String issuer,
        /** access 토큰 유효 시간(초). */
        long accessTokenValiditySeconds,
        /** refresh 토큰 유효 시간(초). */
        long refreshTokenValiditySeconds
) {
}
