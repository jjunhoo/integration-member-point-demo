package com.retail.membership.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p><b>용도:</b> JWT 서명 키·만료시간 등 application.yml 바인딩 설정.</p>
 *
 * JWT 설정 (application.yml: retail.auth.jwt.*).
 */
@ConfigurationProperties(prefix = "retail.auth.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTokenValiditySeconds,
        long refreshTokenValiditySeconds
) {
}
