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
 * 인증 API (permitAll).
 *
 * <ul>
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
