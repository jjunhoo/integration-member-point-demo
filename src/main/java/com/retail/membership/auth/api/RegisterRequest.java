package com.retail.membership.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * <p><b>용도:</b> 로컬 회원가입 API 요청 DTO.</p>
 *
 * <p>채널은 가입 시 받지 않는다. 채널 연결은 별도 API 로 처리한다.
 */
public record RegisterRequest(
        /** 로컬 로그인 ID (영문/숫자/_ , 4~30자). */
        @NotBlank
        @Size(min = 4, max = 30)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "loginId는 영문, 숫자, _ 만 사용할 수 있습니다")
        String loginId,

        /** 비밀번호 평문 (8자 이상). */
        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        /** 이메일 (선택). */
        @Email String email,

        /** 이름 (선택). */
        @Size(max = 50) String name
) {
}
