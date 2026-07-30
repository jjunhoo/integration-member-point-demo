package com.retail.membership.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * <p><b>용도:</b> JWT(HS256) 발급·파싱·타입 검증을 수행하는 컴포넌트.</p>
 *
 * JWT 발급/검증 provider (HS256).
 *
 * <p>토큰 클레임:
 * <ul>
 *   <li>{@code sub}     : 통합 회원 ID</li>
 *   <li>{@code channel} : 로그인한 비즈니스 채널</li>
 *   <li>{@code roles}   : 권한 목록</li>
 *   <li>{@code type}    : ACCESS / REFRESH (토큰 오용 방지)</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final SecretKey key;

    /** 설정의 시크릿으로 HS256 서명 키를 초기화한다. */
    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        // HS256: 시크릿은 최소 256bit(32byte) 이상이어야 한다.
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /** access + refresh 토큰 쌍을 발급한다. */
    public TokenPair issue(String memberId, String channel, List<String> roles) {
        String access = build(memberId, channel, roles, TokenType.ACCESS,
                properties.accessTokenValiditySeconds());
        String refresh = build(memberId, channel, roles, TokenType.REFRESH,
                properties.refreshTokenValiditySeconds());
        return new TokenPair(access, refresh, properties.accessTokenValiditySeconds());
    }

    /** 지정 타입·만료 시간으로 JWT 문자열을 생성한다. */
    private String build(String memberId, String channel, List<String> roles,
                         TokenType type, long validitySeconds) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .issuer(properties.issuer())
                .subject(memberId)
                .claim("roles", roles)
                .claim("type", type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(key);
        // 채널은 레거시 계정 매핑용이며 로컬 인증에는 없을 수 있다.
        if (channel != null && !channel.isBlank()) {
            builder.claim("channel", channel);
        }
        return builder.compact();
    }

    /** 토큰을 검증하고 클레임을 파싱한다. 유효하지 않으면 {@link JwtException}. */
    public Claims parse(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    /** 토큰이 기대한 타입(ACCESS/REFRESH)인지 확인한다. */
    public boolean isType(Claims claims, TokenType expected) {
        return expected.name().equals(claims.get("type", String.class));
    }
}
