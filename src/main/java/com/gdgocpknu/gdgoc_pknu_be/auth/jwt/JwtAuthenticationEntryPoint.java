package com.gdgocpknu.gdgoc_pknu_be.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Spring Security 기본 401(HTML)을 API 명세서 1-7의 공통 에러 모양으로 바꾼다.
 * `/api/admin/**`에 토큰 없이 접근했을 때 여기가 걸린다 — {@link JwtAuthenticationFilter}가 남겨 둔
 * 요청 속성으로 만료(`TOKEN_EXPIRED`)와 그 외(`UNAUTHORIZED`)를 구분하고, 속성이 없으면(토큰 자체가 없음)
 * `UNAUTHORIZED`("로그인이 필요합니다")를 기본값으로 쓴다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        Object attribute = request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE);
        ErrorCode code = attribute instanceof ErrorCode ec ? ec : ErrorCode.UNAUTHORIZED;

        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(code));
    }
}
