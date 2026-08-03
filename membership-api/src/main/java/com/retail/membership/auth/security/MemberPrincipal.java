package com.retail.membership.auth.security;

import java.util.List;

/**
 * <p><b>용도:</b> 인증된 회원을 나타내는 Security principal (memberId/channel/roles).</p>
 */
public record MemberPrincipal(
        /** 통합 회원 ID (JWT sub). */
        String memberId,
        /** 로그인 시 채널 클레임 (없으면 null). */
        String channel,
        /** 권한 목록 (예: ROLE_MEMBER). */
        List<String> roles
) {
}
