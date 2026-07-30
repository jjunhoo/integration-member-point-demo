package com.retail.membership.auth.api;

import com.retail.membership.auth.application.AuthService;
import com.retail.membership.auth.jwt.TokenPair;
import com.retail.membership.auth.security.MemberPrincipal;
import com.retail.membership.auth.social.SocialProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러.
 *
 * <p>로컬(loginId/password) · 네이버 OAuth · 소셜 access-token 로그인과
 * JWT 발급/갱신/로그아웃, 비밀번호 변경을 담당한다.
 * 대부분 {@code permitAll} 이며, 비밀번호 변경·로그아웃은 Bearer JWT 가 필요하다.
 *
 * <ul>
 *   <li>{@code POST /api/v1/auth/register}                  : 로컬 회원가입</li>
 *   <li>{@code POST /api/v1/auth/login}                     : 로컬 로그인</li>
 *   <li>{@code GET  /api/v1/auth/oauth/naver/authorize-url} : 네이버 인가 URL 발급</li>
 *   <li>{@code POST /api/v1/auth/oauth/naver}               : 네이버 인가 코드 로그인</li>
 *   <li>{@code POST /api/v1/auth/password}                  : 비밀번호 변경 (Bearer)</li>
 *   <li>{@code POST /api/v1/auth/social/{provider}}         : 소셜 토큰 직접 로그인</li>
 *   <li>{@code POST /api/v1/auth/refresh}                   : 리프레시 토큰으로 재발급</li>
 *   <li>{@code POST /api/v1/auth/logout}                    : 로그아웃 (리프레시 폐기)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로컬 회원가입.
     *
     * <p>통합 회원 + 로컬 자격증명({@code LocalCredential})을 생성하고
     * access/refresh 토큰 쌍을 반환한다. 채널 계정은 여기서 만들지 않는다.
     */
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        TokenPair pair = authService.register(
                request.loginId(),
                request.password(),
                request.email(),
                request.name());
        return ResponseEntity.ok(TokenResponse.from(pair));
    }

    /**
     * 로컬 로그인.
     *
     * <p>loginId/password 검증 후 JWT 토큰 쌍을 발급한다.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenPair pair = authService.login(request.loginId(), request.password());
        return ResponseEntity.ok(TokenResponse.from(pair));
    }

    /**
     * 네이버 OAuth 인가 URL 생성.
     *
     * <p>CSRF 방지용 {@code state} 를 Redis 에 저장하고,
     * 프론트가 리다이렉트할 네이버 로그인 URL 을 반환한다.
     */
    @GetMapping("/oauth/naver/authorize-url")
    public ResponseEntity<NaverAuthorizeUrlResponse> naverAuthorizeUrl() {
        return ResponseEntity.ok(authService.createNaverAuthorizeUrl());
    }

    /**
     * 네이버 인가 코드로 로그인/가입.
     *
     * <p>콜백으로 받은 {@code code}/{@code state} 로 네이버 토큰·프로필을 조회하고,
     * {@code social_account} 매핑으로 통합 회원을 찾거나 신규 생성한 뒤 JWT 를 발급한다.
     */
    @PostMapping("/oauth/naver")
    public ResponseEntity<TokenResponse> naverOAuthLogin(@Valid @RequestBody NaverOAuthRequest request) {
        TokenPair pair = authService.loginWithNaverCode(request.code(), request.state());
        return ResponseEntity.ok(TokenResponse.from(pair));
    }

    /**
     * 비밀번호 변경 (인증 필요).
     *
     * <p>현재 비밀번호 확인 후 새 비밀번호로 교체한다.
     * JWT 의 memberId 기준으로 본인 계정만 변경한다.
     */
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        authService.changePassword(principal.memberId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    /**
     * 소셜 provider access-token 으로 직접 로그인.
     *
     * <p>클라이언트가 이미 보유한 카카오/네이버/애플 토큰을 넘겨
     * 프로필을 조회하고 통합 회원으로 JWT 를 발급한다.
     * (브라우저 OAuth 콜백 대신 API 연동/테스트용)
     */
    @PostMapping("/social/{provider}")
    public ResponseEntity<TokenResponse> socialLogin(
            @PathVariable("provider") SocialProvider provider,
            @Valid @RequestBody SocialLoginRequest request) {
        TokenPair pair = authService.socialLogin(provider, request.token());
        return ResponseEntity.ok(TokenResponse.from(pair));
    }

    /**
     * 토큰 재발급.
     *
     * <p>유효한 refreshToken 으로 새 access/refresh 쌍을 발급하고,
     * 기존 refresh 는 교체(회전)한다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenPair pair = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(TokenResponse.from(pair));
    }

    /**
     * 로그아웃.
     *
     * <p>서버에 저장된 refreshToken 을 폐기한다. accessToken 은 만료까지 유효할 수 있다.
     * principal 이 없으면 본문 없이 204 만 반환한다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal MemberPrincipal principal) {
        if (principal != null) {
            authService.logout(principal.memberId());
        }
        return ResponseEntity.noContent().build();
    }
}
