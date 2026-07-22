package com.retail.membership.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 요청.
 *
 * @param currentPassword 현재 비밀번호
 * @param newPassword     새 비밀번호 (8자 이상)
 */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,

        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword
) {
}
