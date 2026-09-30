package com.gdgocpknu.gdgoc_pknu_be.auth.jwt;

import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * `Authorization: Bearer <token>`을 검증해 인증 컨텍스트를 채운다. 토큰이 없으면 그냥 다음 필터로 넘긴다 —
 * 공개 GET은 인증이 필요 없으므로 여기서 막을 이유가 없고, 관리자 경로는 뒤의 인가 단계에서 막힌다.
 *
 * <p>검증 실패 이유(만료 · 그 외)를 요청 속성에 남겨 두면 {@link JwtAuthenticationEntryPoint}가
 * 401 응답의 `code`를 `TOKEN_EXPIRED`와 `UNAUTHORIZED`로 구분할 수 있다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String ERROR_CODE_ATTRIBUTE = "com.gdgocpknu.gdgoc_pknu_be.auth.jwt.errorCode";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            authenticate(request, header.substring("Bearer ".length()));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            String loginId = jwtProvider.parseSubject(token);
            var authentication = new UsernamePasswordAuthenticationToken(
                    loginId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException e) {
            request.setAttribute(ERROR_CODE_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            request.setAttribute(ERROR_CODE_ATTRIBUTE, ErrorCode.UNAUTHORIZED);
        }
    }
}
