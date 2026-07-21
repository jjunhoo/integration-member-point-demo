package com.retail.membership.auth.application;

import com.retail.membership.auth.jwt.JwtProperties;
import com.retail.membership.auth.jwt.JwtTokenProvider;
import com.retail.membership.auth.jwt.TokenPair;
import com.retail.membership.auth.jwt.TokenType;
import com.retail.membership.auth.social.SocialLoginClientRegistry;
import com.retail.membership.auth.social.SocialProvider;
import com.retail.membership.auth.social.SocialUserInfo;
import com.retail.membership.member.application.MemberService;
import com.retail.membership.member.domain.Channel;
import com.retail.membership.member.domain.IntegratedMember;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 인증 애플리케이션 서비스.
 *
 * <p>소셜 로그인 → 통합 회원 해석 → JWT 발급의 오케스트레이션을 담당한다.
 * refresh 토큰 재발급/로그아웃(회수)도 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 기본 회원 권한. 등급/역할 세분화 시 확장. */
    private static final List<String> DEFAULT_ROLES = List.of("ROLE_MEMBER");

    private final SocialLoginClientRegistry socialClients;
    private final MemberService memberService;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenStore refreshTokenStore;

    /**
     * 소셜 로그인. provider SDK 로 획득한 토큰으로 사용자를 식별하고 JWT 를 발급한다.
     *
     * @param provider     소셜 provider (KAKAO/NAVER/APPLE)
     * @param loginChannel 로그인한 비즈니스 채널
     * @param token        access token(kakao/naver) 또는 id_token(apple)
     */
    public TokenPair socialLogin(SocialProvider provider, Channel loginChannel, String token) {
        // 1) provider 전략으로 표준 사용자 정보 획득
        SocialUserInfo userInfo = socialClients.get(provider).fetchUserInfo(token);

        // 2) 통합 회원 해석(없으면 신규 가입)
        IntegratedMember member = memberService.resolveOrRegisterBySocial(userInfo, loginChannel);

        // 3) JWT 발급 + refresh 저장
        return issueAndStore(member.getId(), loginChannel.name());
    }

    /** refresh 토큰으로 access/refresh 재발급 (rotation). */
    public TokenPair refresh(String refreshToken) {
        Claims claims;
        try {
            claims = tokenProvider.parse(refreshToken);
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 refresh 토큰");
        }
        if (!tokenProvider.isType(claims, TokenType.REFRESH)) {
            throw new IllegalArgumentException("refresh 토큰이 아닙니다");
        }
        String memberId = claims.getSubject();
        // 저장소의 최신 refresh 와 대조 (탈취/재사용 방지)
        if (!refreshTokenStore.matches(memberId, refreshToken)) {
            throw new IllegalArgumentException("만료되었거나 회수된 refresh 토큰");
        }
        String channel = claims.get("channel", String.class);
        return issueAndStore(memberId, channel);
    }

    /** 로그아웃: refresh 토큰 회수. */
    public void logout(String memberId) {
        refreshTokenStore.revoke(memberId);
    }

    private TokenPair issueAndStore(String memberId, String channel) {
        TokenPair pair = tokenProvider.issue(memberId, channel, DEFAULT_ROLES);
        refreshTokenStore.save(memberId, pair.refreshToken(),
                jwtProperties.refreshTokenValiditySeconds());
        return pair;
    }
}
