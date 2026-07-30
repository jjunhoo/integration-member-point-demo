package com.retail.membership.auth.social;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p><b>용도:</b> 카카오/네이버/애플 엔드포인트·시크릿 등 소셜 설정 바인딩.</p>
 */
@ConfigurationProperties(prefix = "retail.social")
public record SocialLoginProperties(
        /** 카카오 설정. */
        Kakao kakao,
        /** 네이버 설정. */
        Naver naver,
        /** 애플 설정. */
        Apple apple
) {
    /** 카카오 사용자 정보 API 설정. */
    public record Kakao(
            /** 카카오 사용자 정보 조회 URI. */
            String userInfoUri
    ) {
    }

    /** 네이버 OAuth + 사용자 정보 API 설정. */
    public record Naver(
            /** 네이버 앱 Client ID. */
            String clientId,
            /** 네이버 앱 Client Secret (서버 전용). */
            String clientSecret,
            /** 인가 코드 콜백 URL. */
            String redirectUri,
            /** 인가 화면 URL. */
            String authorizeUri,
            /** 토큰 발급 URL. */
            String tokenUri,
            /** 프로필 조회 URL. */
            String userInfoUri
    ) {
    }

    /** 애플 id_token 검증용 설정. */
    public record Apple(
            /** id_token issuer. */
            String issuer,
            /** id_token audience (앱 client id). */
            String audience
    ) {
    }
}
