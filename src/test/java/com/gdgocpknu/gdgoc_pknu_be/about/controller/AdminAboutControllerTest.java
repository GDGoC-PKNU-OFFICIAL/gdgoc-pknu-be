package com.gdgocpknu.gdgoc_pknu_be.about.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgocpknu.gdgoc_pknu_be.about.dto.AboutRequest;
import com.gdgocpknu.gdgoc_pknu_be.about.dto.AboutRequest.ActivityRequest;
import com.gdgocpknu.gdgoc_pknu_be.about.dto.AboutRequest.FaqRequest;
import com.gdgocpknu.gdgoc_pknu_be.about.dto.AboutRequest.RuleRequest;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** About 조회 · 수정을 실제 PostgreSQL(JSONB) 위에서 검증한다. */
@IntegrationTest
@Transactional
class AdminAboutControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    EntityManager em;

    @Test
    void 저장한_적이_없으면_공개_관리자_조회_모두_404다() throws Exception {
        mockMvc.perform(get("/api/about")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admin/about").with(user("admin"))).andExpect(status().isNotFound());
    }

    @Test
    void 인증_없이_수정하면_401이다() throws Exception {
        mockMvc.perform(put("/api/admin/about").contentType(MediaType.APPLICATION_JSON).content(json(request("a"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 처음_저장하면_행이_생기고_다시_저장하면_통째로_바뀐다() throws Exception {
        mockMvc.perform(put("/api/admin/about").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request("code-of-conduct"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity[0]").value("첫 문단"))
                .andExpect(jsonPath("$.activities[0].layout").value("square"))
                .andExpect(jsonPath("$.activities[0].secondImageUrl").doesNotExist())
                .andExpect(jsonPath("$.rules[0].slug").value("code-of-conduct"))
                .andExpect(jsonPath("$.faq[0].answer").value("답변"));

        em.flush();
        em.clear();

        AboutRequest second = new AboutRequest(List.of("바뀐 문단"), request("x").activities(), List.of(), List.of());
        mockMvc.perform(put("/api/admin/about").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(second)))
                .andExpect(status().isOk());

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity[0]").value("바뀐 문단"))
                .andExpect(jsonPath("$.rules").isEmpty())
                .andExpect(jsonPath("$.faq").isEmpty());
    }

    @Test
    void 규칙_슬러그가_겹치면_400이다() throws Exception {
        AboutRequest base = request("dup");
        AboutRequest duplicated = new AboutRequest(base.identity(), base.activities(),
                List.of(base.rules().get(0), base.rules().get(0)), base.faq());

        mockMvc.perform(put("/api/admin/about").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(duplicated)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='rules[1].slug')]").exists());
    }

    private String json(AboutRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private static AboutRequest request(String ruleSlug) {
        return new AboutRequest(
                List.of("첫 문단"),
                List.of(new ActivityRequest("activity", "정기 세션", "격주", "설명", "square",
                        "/images/activities/session-photo.webp", null, null, "auto")),
                List.of(new RuleRequest(ruleSlug, "행동 강령", "book", "## 제목\n- 항목")),
                List.of(new FaqRequest("질문", "답변")));
    }
}
