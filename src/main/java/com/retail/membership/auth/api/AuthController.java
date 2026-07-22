package com.retail.membership.auth.api;

import com.retail.membership.auth.application.AuthService;
import com.retail.membership.auth.jwt.TokenPair;
import com.retail.membership.auth.security.MemberPrincipal;
import com.retail.membership.auth.social.SocialProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API.
 *
 * <ul>
 *   <li>{@code POST /api/v1/auth/register}          : 로컬 회원가입</li>
 *   <li>{@code POST /api/v1/auth/login}             : 로컬 로그인</li>
 *   <li>{@code POST /api/v1/auth/password}          : 비밀번호 변경 (Bearer)</li>
 *   <li>{@code POST /api/v1/auth/social/{provider}} : 소셜 로그인(카카오/네이버/애플)</li>
 *   <li>{@code POST /api/v1/auth/refresh}           : 토큰 재발급</li>
 *   <li>{@code POST /api/v1/auth/logout}            : 로그아웃(refresh 회수)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 로컬 회원가입. 성공 시 JWT 를 바로 발급한다. */
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        TokenPair pair = authService.register(
                request.loginId(),
                request.password(),
                request.channel(),
                request.email(),
                request.name());
        return ResponseEntity.ok(TokenResponse.from(pair));
    }

    /** 로컬 로그인. */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenPair pair = authService.login(request.loginId(), request.password(), request.channel());
        return ResponseEntity.ok(TokenResponse.from(pair));
    }

    /** 비밀번호 변경. 성공 시 refresh 회수. */
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
     * 소셜 로그인. 경로변수 provider 로 카카오/네이버/애플을 구분한다.
     * 예: {@code POST /api/v1/auth/social/kakao}
     */
    @PostMapping("/social/{provider}")
    public ResponseEntity<TokenResponse> socialLogin(
            @PathVariable("provider") SocialProvider provider,
            @Valid @RequestBody SocialLoginRequest request) {
        TokenPair pair = authService.socialLogin(provider, request.channel(), request.token());
        return ResponseEntity.ok(TokenResponse.from(pair));
    }

    /** access/refresh 재발급. */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenPair pair = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(TokenResponse.from(pair));
    }

    /** 로그아웃: 인증된 회원의 refresh 토큰을 회수한다. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal MemberPrincipal principal) {
        if (principal != null) {
            authService.logout(principal.memberId());
        }
        return ResponseEntity.noContent().build();
    }
}
