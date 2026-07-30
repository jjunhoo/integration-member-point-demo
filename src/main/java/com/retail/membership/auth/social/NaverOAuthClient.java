package com.retail.membership.auth.social;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 네이버 OAuth2 인가 코드 → access token 교환.
 */
@Slf4j
@Component
public class NaverOAuthClient {

    private final SocialLoginProperties.Naver naver;
    private final RestClient restClient;

    public NaverOAuthClient(SocialLoginProperties properties) {
        this.naver = properties.naver();
        this.restClient = RestClient.create();
    }

    public void requireConfigured() {
        if (!StringUtils.hasText(naver.clientId()) || !StringUtils.hasText(naver.clientSecret())) {
            throw new SocialAuthException(
                    "네이버 OAuth 설정이 없습니다. NAVER_CLIENT_ID / NAVER_CLIENT_SECRET 을 설정하세요.");
        }
        if (!StringUtils.hasText(naver.redirectUri())) {
            throw new SocialAuthException("네이버 redirect URI 가 없습니다.");
        }
    }

    public String buildAuthorizeUrl(String state) {
        requireConfigured();
        return UriComponentsBuilder
                .fromUriString(naver.authorizeUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", naver.clientId())
                .queryParam("redirect_uri", naver.redirectUri())
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();
    }

    @SuppressWarnings("unchecked")
    public String exchangeCodeForAccessToken(String code, String state) {
        requireConfigured();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", naver.clientId());
        form.add("client_secret", naver.clientSecret());
        form.add("code", code);
        form.add("state", state);

        Map<String, Object> body;
        try {
            body = restClient.post()
                    .uri(naver.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.warn("[NaverOAuth] 토큰 교환 실패: {}", e.getMessage());
            throw new SocialAuthException("네이버 토큰 교환에 실패했습니다", e);
        }

        if (body == null || body.get("access_token") == null) {
            Object error = body == null ? null : body.get("error");
            Object description = body == null ? null : body.get("error_description");
            throw new SocialAuthException(
                    "네이버 토큰 교환 실패: " + error + " " + description);
        }
        return String.valueOf(body.get("access_token"));
    }
}
