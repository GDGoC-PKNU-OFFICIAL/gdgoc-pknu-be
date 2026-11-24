package com.gdgocpknu.gdgoc_pknu_be.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUser;
import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUserRepository;
import com.gdgocpknu.gdgoc_pknu_be.auth.service.LoginAttemptLimiter;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 로그인 API에서 시도 제한이 걸리는지 확인한다. 한도를 3으로 낮춘 별도 컨텍스트라 다른 테스트의 로그인 시도와
 * 섞이지 않는다. 계정은 테스트 트랜잭션이 롤백하고, 제한기의 메모리 상태는 매 테스트 앞에서 비운다.
 */
@IntegrationTest
@Transactional
@TestPropertySource(properties = "app.login-throttle.max-attempts=3")
class LoginThrottleIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    AdminUserRepository adminUserRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter.reset();
        adminUserRepository.save(AdminUser.create("admin", passwordEncoder.encode("correct-password")));
    }

    @Test
    void 한도까지_틀리면_401이고_넘으면_맞는_비밀번호도_429_LOGIN_LOCKED와_Retry_After다() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("admin", "wrong-" + i).andExpect(status().isUnauthorized());
        }

        login("admin", "correct-password")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_LOCKED"))
                .andExpect(header().string("Retry-After", "300"));
    }

    @Test
    void 잠긴_동안에는_없는_아이디도_같은_429라_계정_존재_여부가_드러나지_않는다() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("nobody", "x").andExpect(status().isUnauthorized());
        }

        login("nobody", "x").andExpect(status().isTooManyRequests());
        login("admin", "x").andExpect(status().isTooManyRequests());
    }

    @Test
    void 중간에_성공하면_횟수가_초기화되어_계속_로그인할_수_있다() throws Exception {
        for (int round = 0; round < 3; round++) {
            login("admin", "wrong-a").andExpect(status().isUnauthorized());
            login("admin", "wrong-b").andExpect(status().isUnauthorized());
            login("admin", "correct-password").andExpect(status().isOk());
        }
    }

    @Test
    void 잠금이_풀리면_다시_로그인할_수_있다() throws Exception {
        for (int i = 0; i < 4; i++) {
            login("admin", "wrong").andReturn();
        }
        login("admin", "correct-password").andExpect(status().isTooManyRequests());

        limiter.reset();   // 5분이 지난 것과 같은 상태

        login("admin", "correct-password").andExpect(status().isOk());
    }

    private ResultActions login(String loginId, String password) throws Exception {
        return mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"loginId\": \"%s\", \"password\": \"%s\" }".formatted(loginId, password)));
    }
}
