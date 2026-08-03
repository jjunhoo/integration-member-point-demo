package com.retail.membership.auth.social;

/**
 * <p><b>용도:</b> 지원 소셜 로그인 provider 상수 (KAKAO/NAVER/APPLE).</p>
 *
 * 지원하는 소셜 로그인 provider. 신규 provider 추가 시 여기에 상수만 추가하면 된다.
 */
public enum SocialProvider {
    /** 카카오 로그인. */
    KAKAO,
    /** 네이버 로그인. */
    NAVER,
    /** Apple 로그인. */
    APPLE
}
