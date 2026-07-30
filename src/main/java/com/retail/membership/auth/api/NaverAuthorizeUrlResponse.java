package com.retail.membership.auth.api;

/**
 * 네이버 인가 URL 응답.
 *
 * @param authorizeUrl 브라우저 리다이렉트 대상
 * @param state        CSRF 검증용 state
 */
public record NaverAuthorizeUrlResponse(
        String authorizeUrl,
        String state
) {
}
