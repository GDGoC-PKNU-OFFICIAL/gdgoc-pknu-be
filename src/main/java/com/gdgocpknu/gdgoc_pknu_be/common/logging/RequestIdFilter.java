package com.gdgocpknu.gdgoc_pknu_be.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 id를 붙여 그 요청에서 나온 로그 줄을 묶는다 (BACK_ARCHITECTURE 8장 28번).
 * id는 MDC `requestId`에 두고 로그 패턴(application.yml `logging.pattern.level`)이 찍는다.
 * 응답 헤더 `X-Request-Id`로도 돌려주므로, 프론트(BFF)가 같은 헤더를 보내면 양쪽 로그를 같은 id로 찾을 수 있다.
 * 응답 본문(명세 1-7 에러 모양)은 바꾸지 않는다.
 *
 * <p>Spring Security보다 먼저 돌아야 401 · 403 응답에도 id가 붙는다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    /** 받은 값이 로그를 오염시키지 않도록(줄바꿈 · 긴 문자열) 형식을 제한한다. 맞지 않으면 새로 만든다. */
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    /** 외부 cron이 몇 분마다 호출하므로 완료 로그에서 뺀다. */
    private static final String HEALTH_PATH = "/api/health";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = resolve(request.getHeader(HEADER));
        long startedAt = System.nanoTime();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            if (!HEALTH_PATH.equals(request.getRequestURI())) {
                long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
                log.info("{} {} {} {}ms", request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
            }
            MDC.remove(MDC_KEY);
        }
    }

    static String resolve(String incoming) {
        if (incoming != null && ALLOWED.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
