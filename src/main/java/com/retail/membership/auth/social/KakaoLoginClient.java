package com.retail.membership.auth.social;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 카카오 로그인 연동.
 *
 * <p>프론트엔드가 카카오 SDK 로 획득한 access token 을 받아
 * 카카오 사용자 정보 API 를 호출해 사용자를 식별한다.
 */
@Slf4j
@Component
public class KakaoLoginClient implements SocialLoginClient {

    private final RestClient restClient;
    private final String userInfoUri;

    public KakaoLoginClient(SocialLoginProperties properties) {
        this.userInfoUri = properties.kakao().userInfoUri();
        this.restClient = RestClient.create();
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SocialUserInfo fetchUserInfo(String accessToken) {
        // GET https://kapi.kakao.com/v2/user/me  (Authorization: Bearer {accessToken})
        Map<String, Object> body = restClient.get()
                .uri(userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        if (body == null || body.get("id") == null) {
            throw new SocialAuthException("카카오 사용자 정보 조회 실패");
        }

        String providerUserId = String.valueOf(body.get("id"));
        Map<String, Object> account = (Map<String, Object>) body.getOrDefault("kakao_account", Map.of());
        Map<String, Object> profile = (Map<String, Object>) account.getOrDefault("profile", Map.of());
        String email = (String) account.get("email");
        String name = (String) profile.get("nickname");

        return new SocialUserInfo(SocialProvider.KAKAO, providerUserId, email, name);
    }
}
