package com.retail.membership.auth.api;

import com.retail.membership.member.domain.Channel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 로컬 회원가입 요청.
 *
 * @param loginId  로그인 ID (영문/숫자/_)
 * @param password 비밀번호 (8자 이상)
 * @param channel  로그인 비즈니스 채널
 * @param email    선택 이메일
 * @param name     선택 이름
 */
public record RegisterRequest(
        @NotBlank
        @Size(min = 4, max = 30)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "loginId는 영문, 숫자, _ 만 사용할 수 있습니다")
        String loginId,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotNull Channel channel,

        @Email String email,

        @Size(max = 50) String name
) {
}
