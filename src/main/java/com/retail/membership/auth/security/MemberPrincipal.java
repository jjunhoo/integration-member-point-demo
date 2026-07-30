package com.retail.membership.auth.security;

import java.util.List;

/**
 * <p><b>용도:</b> 인증된 회원을 나타내는 Security principal (memberId/channel/roles).</p>
 *
 * 인증된 회원 주체. SecurityContext 의 Authentication principal 로 사용한다.
 *
 * @param memberId 통합 회원 ID
 * @param channel  로그인한 비즈니스 채널
 * @param roles    권한 목록
 */
public record MemberPrincipal(
        String memberId,
        String channel,
        List<String> roles
) {
}
