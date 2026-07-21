package com.retail.membership.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
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
