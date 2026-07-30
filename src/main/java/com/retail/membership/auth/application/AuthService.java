package com.retail.membership.auth.application;

import com.retail.membership.auth.api.NaverAuthorizeUrlResponse;
import com.retail.membership.auth.jwt.JwtProperties;
import com.retail.membership.auth.jwt.JwtTokenProvider;
import com.retail.membership.auth.jwt.TokenPair;
import com.retail.membership.auth.jwt.TokenType;
import com.retail.membership.auth.local.LocalCredential;
import com.retail.membership.auth.local.LocalCredentialRepository;
import com.retail.membership.auth.social.NaverOAuthClient;
import com.retail.membership.auth.social.OAuthStateStore;
import com.retail.membership.auth.social.SocialAuthException;
import com.retail.membership.auth.social.SocialLoginClientRegistry;
import com.retail.membership.auth.social.SocialProvider;
import com.retail.membership.auth.social.SocialUserInfo;
import com.retail.membership.member.application.MemberService;
import com.retail.membership.member.domain.IntegratedMember;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 인증 애플리케이션 서비스.
 *
 * <p>소셜/로컬 로그인 → 통합 회원 해석 → JWT 발급의 오케스트레이션을 담당한다.
 * refresh 토큰 재발급/로그아웃(회수)/비밀번호 변경도 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 기본 회원 권한. 등급/역할 세분화 시 확장. */
    private static final List<String> DEFAULT_ROLES = List.of("ROLE_MEMBER");

    private static final String LOGIN_FAILED_MESSAGE = "아이디 또는 비밀번호가 올바르지 않습니다";

    private final SocialLoginClientRegistry socialClients;
    private final MemberService memberService;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenStore refreshTokenStore;
    private final LocalCredentialRepository localCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final NaverOAuthClient naverOAuthClient;
    private final OAuthStateStore oAuthStateStore;

    /**
     * 소셜 로그인. provider 토큰으로 사용자를 식별하고 JWT 를 발급한다.
     *
     * @param provider 소셜 provider (KAKAO/NAVER/APPLE)
     * @param token    access token(kakao/naver) 또는 id_token(apple)
     */
    public TokenPair socialLogin(SocialProvider provider, String token) {
        SocialUserInfo userInfo = socialClients.get(provider).fetchUserInfo(token);
        IntegratedMember member = memberService.resolveOrRegisterBySocial(userInfo);
        return issueAndStore(member.getId(), null);
    }

    /** 네이버 인가 URL + state 발급. */
    public NaverAuthorizeUrlResponse createNaverAuthorizeUrl() {
        String state = oAuthStateStore.issue(SocialProvider.NAVER.name());
        String authorizeUrl = naverOAuthClient.buildAuthorizeUrl(state);
        return new NaverAuthorizeUrlResponse(authorizeUrl, state);
    }

    /**
     * 네이버 인가 코드 로그인.
     * state 검증 → code 로 access token 교환 → 프로필 조회 → JWT 발급.
     */
    public TokenPair loginWithNaverCode(String code, String state) {
        if (!oAuthStateStore.consume(state, SocialProvider.NAVER.name())) {
            throw new SocialAuthException("유효하지 않거나 만료된 OAuth state 입니다");
        }
        String accessToken = naverOAuthClient.exchangeCodeForAccessToken(code, state);
        SocialUserInfo userInfo = socialClients.get(SocialProvider.NAVER).fetchUserInfo(accessToken);
        IntegratedMember member = memberService.resolveOrRegisterBySocial(userInfo);
        log.info("[Auth] 네이버 OAuth 로그인 memberId={}", member.getId());
        return issueAndStore(member.getId(), null);
    }

    /**
     * 로컬 회원가입. loginId/password 로 LocalCredential 을 만들고 JWT 를 발급한다.
     */
    @Transactional
    public TokenPair register(String loginId, String password, String email, String name) {
        if (localCredentialRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException("이미 사용 중인 loginId 입니다");
        }

        IntegratedMember member = memberService.registerLocalMember(name, email);
        localCredentialRepository.save(
                LocalCredential.create(member.getId(), loginId, passwordEncoder.encode(password)));

        log.info("[Auth] 로컬 회원가입 완료 memberId={} loginId={}", member.getId(), loginId);
        return issueAndStore(member.getId(), null);
    }

    /** 로컬 로그인. 실패 메시지는 계정 존재 여부를 드러내지 않는다. */
    @Transactional(readOnly = true)
    public TokenPair login(String loginId, String password) {
        LocalCredential credential = localCredentialRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException(LOGIN_FAILED_MESSAGE));

        if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
            throw new IllegalArgumentException(LOGIN_FAILED_MESSAGE);
        }

        log.debug("[Auth] 로컬 로그인 성공 memberId={}", credential.getMemberId());
        return issueAndStore(credential.getMemberId(), null);
    }

    /** 비밀번호 변경. 성공 시 refresh 를 회수하여 재로그인을 유도한다. */
    @Transactional
    public void changePassword(String memberId, String currentPassword, String newPassword) {
        LocalCredential credential = localCredentialRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("로컬 로그인 계정이 없습니다"));

        if (!passwordEncoder.matches(currentPassword, credential.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다");
        }
        if (passwordEncoder.matches(newPassword, credential.getPasswordHash())) {
            throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다");
        }

        credential.changePassword(passwordEncoder.encode(newPassword));
        refreshTokenStore.revoke(memberId);
        log.info("[Auth] 비밀번호 변경 완료 memberId={}", memberId);
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
