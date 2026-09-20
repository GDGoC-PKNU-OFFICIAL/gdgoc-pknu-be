package com.gdgocpknu.gdgoc_pknu_be.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 공개 API는 GET만 열고, 관리자 API는 인증을 요구한다. 규칙 순서가 중요하다: `/api/admin/**`가
 * `GET /api/**`보다 먼저 와야 관리자 조회가 공개로 열리지 않는다.
 *
 * <p>STEP 3 전까지는 인증 수단(JWT 필터)이 없어 `/api/admin/**`는 실제로 아무도 통과하지 못한다.
 * 관리자 API는 MockMvc의 `user()`로 검증하고, STEP 3에서 로그인 permitAll · JWT 필터 · 401 공통 JSON을 추가한다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/admin/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }
}
