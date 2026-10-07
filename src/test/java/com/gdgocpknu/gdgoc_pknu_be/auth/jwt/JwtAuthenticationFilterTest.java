package com.gdgocpknu.gdgoc_pknu_be.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 필터가 토큰 상태별로 인증 컨텍스트 · 요청 속성을 어떻게 남기는지만 본다(Spring 컨텍스트 · DB 불필요).
 * 그 속성을 읽어 401 응답으로 바꾸는 쪽은 {@link JwtAuthenticationEntryPoint}이고, 그건 통합 테스트가 본다.
 */
class JwtAuthenticationFilterTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProvider);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 토큰이_없으면_인증도_속성도_남기지_않고_그냥_통과시킨다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE)).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void 유효한_토큰이면_subject로_인증_컨텍스트를_채운다() throws Exception {
        given(jwtProvider.parseSubject("valid-token")).willReturn("admin");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin");
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE)).isNull();
    }

    @Test
    void 만료된_토큰은_TOKEN_EXPIRED_속성을_남기고_체인은_계속_진행한다() throws Exception {
        given(jwtProvider.parseSubject("expired-token")).willThrow(mock(ExpiredJwtException.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE)).isEqualTo(ErrorCode.TOKEN_EXPIRED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void 위조된_토큰은_UNAUTHORIZED_속성을_남긴다() throws Exception {
        given(jwtProvider.parseSubject("tampered-token")).willThrow(mock(SignatureException.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tampered-token");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE)).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void Bearer_형식이_아니면_토큰을_읽지_않고_통과시킨다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE)).isNull();
    }
}
