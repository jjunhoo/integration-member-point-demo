package com.retail.membership.auth.api;

import com.retail.membership.member.domain.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 소셜 로그인 요청.
 *
 * @param channel 로그인하는 비즈니스 채널 (편의점/슈퍼/홈쇼핑/O4O)
 * @param token   provider SDK 로 획득한 access token(kakao/naver) 또는 id_token(apple)
 */
public record SocialLoginRequest(
        @NotNull Channel channel,
        @NotBlank String token
) {
}
