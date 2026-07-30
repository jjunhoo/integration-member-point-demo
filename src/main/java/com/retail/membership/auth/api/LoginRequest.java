package com.retail.membership.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * 로컬 로그인 요청.
 *
 * <p>채널은 인증 대상이 아니다. 레거시 채널 계정 연결은
 * {@code POST /api/v1/members/me/channels} 로 별도 처리한다.
 */
public record LoginRequest(
        @NotBlank String loginId,
        @NotBlank String password
) {
}
