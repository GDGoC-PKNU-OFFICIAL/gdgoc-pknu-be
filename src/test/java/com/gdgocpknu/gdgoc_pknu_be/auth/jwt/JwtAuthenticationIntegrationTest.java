package com.gdgocpknu.gdgoc_pknu_be.auth.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUser;
import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUserRepository;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 → 실제 토큰 발급 → 그 토큰으로 관리자 API 호출까지 실제 Spring Security 필터 체인으로 검증한다.
 * (다른 관리자 테스트는 `MockMvc`의 `user()`로 인증을 흉내 내지만, 여기서는 필터·엔트리포인트 배선 자체를 본다.)
 */
@IntegrationTest
@Transactional
class JwtAuthenticationIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    AdminUserRepository adminUserRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAccount() {
        adminUserRepository.save(AdminUser.create("admin", passwordEncoder.encode("correct-password")));
    }

    @Test
    void 토큰_없이_관리자_API를_호출하면_401_UNAUTHORIZED_공통_JSON이다() throws Exception {
        mockMvc.perform(get("/api/admin/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    void 위조된_토큰으로_호출하면_401_UNAUTHORIZED다() throws Exception {
        mockMvc.perform(get("/api/admin/projects").header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 공개_GET은_토큰_없이_통과한다() throws Exception {
        mockMvc.perform(get("/api/projects")).andExpect(status().isOk());
    }

    @Test
    void 로그인으로_받은_실제_토큰으로_관리자_API를_호출할_수_있다() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"loginId\": \"admin\", \"password\": \"correct-password\" }"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loginResponse, "$.token");

        mockMvc.perform(get("/api/admin/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }
}
