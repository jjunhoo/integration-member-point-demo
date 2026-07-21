package com.retail.membership.auth.social;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 네이버 로그인 연동.
 *
 * <p>네이버 사용자 정보는 {@code response} 객체 안에 감싸여 반환된다.
 */
@Slf4j
@Component
public class NaverLoginClient implements SocialLoginClient {

    private final RestClient restClient;
    private final String userInfoUri;

    public NaverLoginClient(SocialLoginProperties properties) {
        this.userInfoUri = properties.naver().userInfoUri();
        this.restClient = RestClient.create();
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.NAVER;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SocialUserInfo fetchUserInfo(String accessToken) {
        // GET https://openapi.naver.com/v1/nid/me  (Authorization: Bearer {accessToken})
        Map<String, Object> body = restClient.get()
                .uri(userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        Map<String, Object> response = body == null
                ? null
                : (Map<String, Object>) body.get("response");
        if (response == null || response.get("id") == null) {
            throw new SocialAuthException("네이버 사용자 정보 조회 실패");
        }

        String providerUserId = String.valueOf(response.get("id"));
        String email = (String) response.get("email");
        String name = (String) response.get("name");

        return new SocialUserInfo(SocialProvider.NAVER, providerUserId, email, name);
    }
}
