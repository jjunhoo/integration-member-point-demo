package com.retail.membership.auth.api;

/**
 * <p><b>용도:</b> 네이버 인가 URL과 state 를 프론트에 내려주는 응답 DTO.</p>
 */
public record NaverAuthorizeUrlResponse(
        /** 브라우저가 이동할 네이버 인가 URL. */
        String authorizeUrl,
        /** CSRF 검증용 state (프론트 sessionStorage 보관). */
        String state
) {
}
