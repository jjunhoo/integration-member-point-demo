package com.retail.membership.auth.social;

/**
 * provider 별 응답을 표준화한 소셜 사용자 정보.
 *
 * @param provider       소셜 provider
 * @param providerUserId provider 가 부여한 사용자 고유 ID (동일인 식별 키)
 * @param email          이메일 (없을 수 있음)
 * @param name           이름 (없을 수 있음)
 */
public record SocialUserInfo(
        SocialProvider provider,
        String providerUserId,
        String email,
        String name
) {
}
