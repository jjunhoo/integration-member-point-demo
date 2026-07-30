package com.retail.membership.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 로컬 회원가입 요청.
 *
 * <p>채널은 가입 시 받지 않는다. 채널 계정 매핑은 통합 유스케이스에서 따로 연결한다.
 */
public record RegisterRequest(
        @NotBlank
        @Size(min = 4, max = 30)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "loginId는 영문, 숫자, _ 만 사용할 수 있습니다")
        String loginId,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Email String email,

        @Size(max = 50) String name
) {
}
