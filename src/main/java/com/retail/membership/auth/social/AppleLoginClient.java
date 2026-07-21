package com.retail.membership.auth.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 애플 로그인 연동.
 *
 * <p>애플은 access token 이 아니라 <b>id_token(JWT)</b> 을 검증하는 방식이다.
 * id_token 의 payload 에서 사용자 고유 ID({@code sub})와 이메일을 추출한다.
 *
 * <p><b>운영 필수 사항</b>: 아래 구현은 데모 목적상 서명 검증을 생략하고 클레임만
 * 파싱한다. 실제 운영에서는 반드시 애플 공개키(JWKS: {@code https://appleid.apple.com/auth/keys})
 * 로 서명을 검증하고, {@code iss}(issuer)와 {@code aud}(client_id) 를 확인해야 한다.
 */
@Slf4j
@Component
public class AppleLoginClient implements SocialLoginClient {

    private final ObjectMapper objectMapper;
    private final String expectedIssuer;
    private final String expectedAudience;

    public AppleLoginClient(ObjectMapper objectMapper, SocialLoginProperties properties) {
        this.objectMapper = objectMapper;
        this.expectedIssuer = properties.apple().issuer();
        this.expectedAudience = properties.apple().audience();
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.APPLE;
    }

    @Override
    public SocialUserInfo fetchUserInfo(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                throw new SocialAuthException("애플 id_token 형식 오류");
            }
            // JWT payload(2번째 세그먼트) base64url 디코딩
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);

            // 운영에서는 서명 검증 + iss/aud 검증이 필수. 여기서는 iss/aud 만 방어적으로 확인.
            validateClaim(claims, "iss", expectedIssuer);
            validateClaim(claims, "aud", expectedAudience);

            String providerUserId = (String) claims.get("sub");
            if (providerUserId == null) {
                throw new SocialAuthException("애플 id_token 에 sub 클레임이 없습니다");
            }
            String email = (String) claims.get("email");

            return new SocialUserInfo(SocialProvider.APPLE, providerUserId, email, null);
        } catch (SocialAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialAuthException("애플 id_token 파싱 실패", e);
        }
    }

    private void validateClaim(Map<String, Object> claims, String key, String expected) {
        Object actual = claims.get(key);
        if (expected != null && actual != null && !expected.equals(String.valueOf(actual))) {
            throw new SocialAuthException("애플 id_token 클레임 불일치: " + key);
        }
    }
}
