package com.retail.membership.auth.social;

/**
 * <p><b>용도:</b> provider 응답을 통일한 소셜 사용자 정보 값 객체.</p>
 */
public record SocialUserInfo(
        /** 소셜 provider. */
        SocialProvider provider,
        /** provider 가 부여한 사용자 고유 ID. */
        String providerUserId,
        /** 이메일 (없을 수 있음). */
        String email,
        /** 이름 (없을 수 있음). */
        String name
) {
}
