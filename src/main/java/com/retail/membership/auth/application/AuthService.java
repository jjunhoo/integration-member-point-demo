package com.retail.membership.auth.application;

import com.retail.membership.auth.jwt.JwtProperties;
import com.retail.membership.auth.jwt.JwtTokenProvider;
import com.retail.membership.auth.jwt.TokenPair;
import com.retail.membership.auth.jwt.TokenType;
import com.retail.membership.auth.local.LocalCredential;
import com.retail.membership.auth.local.LocalCredentialRepository;
import com.retail.membership.auth.social.SocialLoginClientRegistry;
import com.retail.membership.auth.social.SocialProvider;
import com.retail.membership.auth.social.SocialUserInfo;
import com.retail.membership.member.application.MemberService;
import com.retail.membership.member.domain.Channel;
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

    /**
     * 로컬 회원가입. loginId/password 로 LocalCredential 을 만들고 JWT 를 발급한다.
     */
    @Transactional
    public TokenPair register(String loginId, String password, Channel channel, String email, String name) {
        if (localCredentialRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException("이미 사용 중인 loginId 입니다");
        }

        String channelMemberNo = "LOCAL:" + loginId;
        IntegratedMember member = memberService.registerLocalMember(name, email, channel, channelMemberNo);

        localCredentialRepository.save(
                LocalCredential.create(member.getId(), loginId, passwordEncoder.encode(password)));

        log.info("[Auth] 로컬 회원가입 완료 memberId={} loginId={} channel={}",
                member.getId(), loginId, channel);
        return issueAndStore(member.getId(), channel.name());
    }

    /**
     * 로컬 로그인. 실패 메시지는 계정 존재 여부를 드러내지 않는다.
     */
    @Transactional
    public TokenPair login(String loginId, String password, Channel channel) {
        LocalCredential credential = localCredentialRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException(LOGIN_FAILED_MESSAGE));

        if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
            throw new IllegalArgumentException(LOGIN_FAILED_MESSAGE);
        }

        // 다른 채널로 로그인해도 채널 계정이 없으면 연결 (소셜 신규와 동일한 편의)
        memberService.ensureChannelLinked(credential.getMemberId(), channel, "LOCAL:" + loginId);

        log.debug("[Auth] 로컬 로그인 성공 memberId={} channel={}", credential.getMemberId(), channel);
        return issueAndStore(credential.getMemberId(), channel.name());
    }

    /**
     * 비밀번호 변경. 성공 시 refresh 를 회수하여 재로그인을 유도한다.
     */
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
