package com.retail.membership.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p><b>용도:</b> 비밀번호 변경 API 요청 DTO.</p>
 */
public record ChangePasswordRequest(
        /** 현재 비밀번호. */
        @NotBlank String currentPassword,

        /** 새 비밀번호 (8자 이상). */
        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword
) {
}
