package com.gdgocpknu.gdgoc_pknu_be.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUser;
import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUserRepository;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** `POST /api/admin/auth/login` (API 명세서 2-2, 4-1). */
@IntegrationTest
@Transactional
class AdminAuthControllerTest {

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
    void 올바른_아이디_비밀번호는_토큰과_만료시각을_반환한다() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "correct-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").value(Matchers.endsWith("+09:00")));
    }

    @Test
    void 비밀번호가_틀리면_401_LOGIN_FAILED다() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    void 존재하지_않는_아이디도_비밀번호_오류와_같은_401_LOGIN_FAILED다() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("no-such-admin", "whatever")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    void 필드가_비어_있으면_400이다() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='loginId')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='password')]").exists());
    }

    private static String loginBody(String loginId, String password) {
        return """
                { "loginId": "%s", "password": "%s" }
                """.formatted(loginId, password);
    }
}
