package com.retail.membership.auth.api;

import com.retail.membership.member.domain.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 로컬 로그인 요청.
 *
 * @param loginId  로그인 ID
 * @param password 비밀번호
 * @param channel  로그인 비즈니스 채널
 */
public record LoginRequest(
        @NotBlank String loginId,
        @NotBlank String password,
        @NotNull Channel channel
) {
}
