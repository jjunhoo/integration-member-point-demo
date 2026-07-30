package com.retail.membership.auth.social;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p><b>용도:</b> 카카오/네이버/애플 엔드포인트·시크릿 등 소셜 설정 바인딩.</p>
 *
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

    /**
     * 네이버 OAuth + 사용자 정보 API 설정.
     *
     * @param clientId     네이버 앱 Client ID
     * @param clientSecret 네이버 앱 Client Secret (서버 전용)
     * @param redirectUri  인가 코드 콜백 URL (프론트 콜백과 일치해야 함)
     * @param authorizeUri 인가 화면 URL
     * @param tokenUri     토큰 발급 URL
     * @param userInfoUri  프로필 조회 URL
     */
    public record Naver(
            String clientId,
            String clientSecret,
            String redirectUri,
            String authorizeUri,
            String tokenUri,
            String userInfoUri
    ) {
    }

    public record Apple(String issuer, String audience) {
    }
}
