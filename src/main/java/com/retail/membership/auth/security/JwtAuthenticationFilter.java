package com.retail.membership.auth.security;

import com.retail.membership.auth.jwt.JwtTokenProvider;
import com.retail.membership.auth.jwt.TokenType;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * <p><b>용도:</b> Bearer access token 을 검증해 SecurityContext 에 인증을 넣는 서블릿 필터.</p>
 *
 * 요청마다 Authorization 헤더의 Bearer access token 을 검증해
 * SecurityContext 에 인증 정보를 설정하는 필터 (Stateless).
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = tokenProvider.parse(token);
                // access 토큰만 API 인증에 사용 (refresh 토큰으로 API 호출 차단)
                if (tokenProvider.isType(claims, TokenType.ACCESS)) {
                    setAuthentication(request, claims);
                }
            } catch (Exception e) {
                // 토큰이 유효하지 않으면 인증 미설정 → 이후 접근 제어에서 401/403 처리
                log.debug("[JWT] 토큰 검증 실패: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private void setAuthentication(HttpServletRequest request, Claims claims) {
        String memberId = claims.getSubject();
        String channel = claims.get("channel", String.class);
        List<String> roles = claims.get("roles", List.class);
        if (roles == null) {
            roles = List.of();
        }

        MemberPrincipal principal = new MemberPrincipal(memberId, channel, roles);
        var authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
