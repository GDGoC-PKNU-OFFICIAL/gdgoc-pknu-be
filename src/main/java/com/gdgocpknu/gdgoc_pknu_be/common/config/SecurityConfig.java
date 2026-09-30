package com.gdgocpknu.gdgoc_pknu_be.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgocpknu.gdgoc_pknu_be.auth.jwt.JwtAuthenticationEntryPoint;
import com.gdgocpknu.gdgoc_pknu_be.auth.jwt.JwtAuthenticationFilter;
import com.gdgocpknu.gdgoc_pknu_be.auth.jwt.JwtProvider;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 공개 GET은 열고, 로그인만 별도로 열고, 그 외 `/api/admin/**`는 JWT 인증을 요구한다.
 * 규칙 순서가 중요하다: `/api/admin/auth/login`(로그인)이 `/api/admin/**`(인증 필요)보다,
 * 그리고 `/api/admin/**`가 `GET /api/**`(공개)보다 먼저 와야 한다.
 *
 * <p>인증 실패(토큰 없음 · 만료 · 위조)는 {@link JwtAuthenticationEntryPoint}가 공통 에러 JSON으로 바꾼다.
 * 이 엔트리포인트는 `/api/admin/**`에만 건다 — Spring Security는 익명 사용자가 `denyAll()`에 걸려도
 * "먼저 인증하라"는 뜻으로 같은 엔트리포인트를 태우는데, 전역으로 걸면 관리자와 무관한 차단
 * (예: 공개 리소스에 지원하지 않는 메서드)까지 403 대신 401로 바뀐다. 그 외 경로는 원래의
 * {@link Http403ForbiddenEntryPoint}(기본 403)를 그대로 쓴다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/admin/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .anyRequest().denyAll())
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint(adminEntryPoint()));
        return http.build();
    }

    private AuthenticationEntryPoint adminEntryPoint() {
        // Spring Security의 최신 대안(PathPatternRequestMatcher)은 이 버전에 아직 없다.
        @SuppressWarnings("removal")
        RequestMatcher adminPaths = new AntPathRequestMatcher("/api/admin/**");
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> byPath = new LinkedHashMap<>();
        byPath.put(adminPaths, new JwtAuthenticationEntryPoint(objectMapper));
        DelegatingAuthenticationEntryPoint delegating = new DelegatingAuthenticationEntryPoint(byPath);
        delegating.setDefaultEntryPoint(new Http403ForbiddenEntryPoint());
        return delegating;
    }
}
