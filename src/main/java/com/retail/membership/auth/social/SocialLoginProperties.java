package com.retail.membership.auth.social;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 소셜 provider 엔드포인트/검증 설정 (application.yml: retail.social.*).
 */
@ConfigurationProperties(prefix = "retail.social")
public record SocialLoginProperties(
        Kakao kakao,
        Naver naver,
        Apple apple
) {
    public record Kakao(String userInfoUri) {
    }

    public record Naver(String userInfoUri) {
    }

    public record Apple(String issuer, String audience) {
    }
}
