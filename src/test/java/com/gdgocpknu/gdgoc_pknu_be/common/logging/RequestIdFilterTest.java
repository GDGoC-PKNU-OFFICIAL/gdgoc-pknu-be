package com.gdgocpknu.gdgoc_pknu_be.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class RequestIdFilterTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 헤더가_없으면_새_id를_만들어_응답_헤더로_돌려준다() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER, matchesPattern("[0-9a-f]{16}")));
    }

    @Test
    void 형식에_맞는_id를_받으면_그대로_쓴다() throws Exception {
        mockMvc.perform(get("/api/health").header(RequestIdFilter.HEADER, "fe-abc.123_X"))
                .andExpect(header().string(RequestIdFilter.HEADER, "fe-abc.123_X"));
    }

    @Test
    void 줄바꿈이나_너무_긴_값은_버리고_새로_만든다() throws Exception {
        mockMvc.perform(get("/api/health").header(RequestIdFilter.HEADER, "abc\nforged log line"))
                .andExpect(header().string(RequestIdFilter.HEADER, matchesPattern("[0-9a-f]{16}")));
        mockMvc.perform(get("/api/health").header(RequestIdFilter.HEADER, "a".repeat(65)))
                .andExpect(header().string(RequestIdFilter.HEADER, matchesPattern("[0-9a-f]{16}")));
    }

    @Test
    void 인증_실패_401에도_id가_붙는다() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(RequestIdFilter.HEADER));
    }

    @Test
    void 요청_처리_중에는_MDC에_id가_있고_끝나면_지운다() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/projects");
        request.addHeader(RequestIdFilter.HEADER, "req-1");

        new RequestIdFilter().doFilter(request, new MockHttpServletResponse(),
                (req, res) -> seen.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertThat(seen.get()).isEqualTo("req-1");
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
