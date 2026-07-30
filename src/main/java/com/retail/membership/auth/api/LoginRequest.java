package com.retail.membership.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * <p><b>용도:</b> 로컬 로그인 API 요청 DTO (loginId/password).</p>
 */
public record LoginRequest(
        /** 로컬 로그인 ID. */
        @NotBlank String loginId,
        /** 비밀번호 평문. */
        @NotBlank String password
) {
}
