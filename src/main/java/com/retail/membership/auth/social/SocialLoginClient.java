package com.retail.membership.auth.social;

/**
 * <p><b>용도:</b> 소셜 provider 별 사용자 조회 전략을 추상화한 인터페이스.</p>
 *
 * 소셜 로그인 provider 연동 전략(Strategy).
 *
 * <p>provider 마다 토큰 검증/사용자 조회 방식이 다르므로 이 인터페이스로 추상화한다.
 * 신규 provider 지원은 이 인터페이스 구현체를 스프링 빈으로 추가하기만 하면 된다
 * (OCP: 기존 코드 수정 없이 확장).
 */
public interface SocialLoginClient {

    /** 이 클라이언트가 담당하는 provider. */
    SocialProvider provider();

    /**
     * 클라이언트가 provider 로부터 받아 전달한 토큰(access token 또는 id_token)을
     * 검증/조회하여 표준 사용자 정보로 변환한다.
     *
     * @param accessToken 프론트엔드가 provider SDK 로 획득해 전달한 토큰
     */
    SocialUserInfo fetchUserInfo(String accessToken);
}
