package com.retail.membership.auth.security;

import com.retail.membership.auth.jwt.JwtProperties;
import com.retail.membership.auth.jwt.JwtTokenProvider;
import com.retail.membership.auth.social.SocialLoginProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * <p><b>용도:</b> Spring Security 필터 체인·공개 경로·JWT 필터·PasswordEncoder 설정.</p>
 *
 * Spring Security 설정 (Stateless JWT 기반).
 *
 * <ul>
 *   <li>세션 미사용(STATELESS): 서버 확장(scale-out) 시 세션 공유 부담 없음</li>
 *   <li>{@link JwtAuthenticationFilter} 를 UsernamePasswordAuthenticationFilter 앞에 배치</li>
 *   <li>인증/소셜로그인/기존 포인트 데모 엔드포인트는 permitAll, 회원 API 는 인증 필요</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, SocialLoginProperties.class})
public class SecurityConfig {

    /** JWT 인증 필터 빈을 등록한다. */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        return new JwtAuthenticationFilter(tokenProvider);
    }

    /** Stateless JWT 기반 Security 필터 체인을 구성한다. */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                // JWT 사용 → CSRF/폼로그인/기본인증/세션 모두 비활성
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 경로
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // 기존 포인트 데모 엔드포인트 (하위 호환 유지)
                        .requestMatchers("/api/v1/membership/**").permitAll()
                        // 데모용 H2 콘솔 (iframe)
                        .requestMatchers("/h2-console/**").permitAll()
                        // 그 외 회원 API 등은 인증 필요
                        .anyRequest().authenticated())
                // H2 콘솔이 iframe 을 쓰므로 same-origin 허용
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // 인증 실패 시 401 반환 (리다이렉트 X)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 로컬 로그인 비밀번호 해시용. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
