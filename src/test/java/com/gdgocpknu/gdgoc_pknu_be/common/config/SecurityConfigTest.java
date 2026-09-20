package com.gdgocpknu.gdgoc_pknu_be.common.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;

/**
 * 인증 수단(JWT)은 STEP 3에서 생긴다. 그 전까지 관리자 API는 이 테스트처럼 `user()`로 인증된 요청을 만들어 검증한다.
 * (여기서는 매핑이 없는 경로를 써서 "보안 규칙"만 확인한다.)
 */
@IntegrationTest
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 공개_GET은_인증_없이_통과한다() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void 공개_경로라도_GET이_아니면_막힌다() throws Exception {
        mockMvc.perform(post("/api/projects")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/projects/1")).andExpect(status().isForbidden());
    }

    @Test
    void 관리자_경로는_GET이어도_인증이_없으면_막힌다() throws Exception {
        mockMvc.perform(get("/api/admin/projects")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/projects")).andExpect(status().isForbidden());
    }

    @Test
    void 인증된_요청은_관리자_경로의_보안_규칙을_통과한다() throws Exception {
        // 매핑이 없는 경로라 보안을 통과하면 404(공통 에러 JSON)가 나온다.
        mockMvc.perform(get("/api/admin/no-such-endpoint").with(user("admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void 나머지_경로는_모두_막힌다() throws Exception {
        mockMvc.perform(get("/internal")).andExpect(status().isForbidden());
    }
}
