package com.gdgocpknu.gdgoc_pknu_be.storage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 주소 발급은 서명만 계산하고 네트워크에 나가지 않으므로 실제 Presigner로 검증한다. */
@IntegrationTest
class AdminUploadControllerTest {

    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void 프로젝트_대상이면_projects_키와_공개_URL과_만료_시각을_반환한다() throws Exception {
        presign("project", "image/webp", 348120)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(matchesPattern("projects/" + UUID_PATTERN + "\\.webp")))
                .andExpect(jsonPath("$.publicUrl").value(
                        matchesPattern("https://test-bucket\\.r2\\.dev/projects/" + UUID_PATTERN + "\\.webp")))
                .andExpect(jsonPath("$.expiresAt").value(matchesPattern(".+\\+09:00")));
    }

    @Test
    void 멤버_대상이면_members_키이고_jpeg_확장자는_jpg다() throws Exception {
        presign("member", "image/jpeg", 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(matchesPattern("members/" + UUID_PATTERN + "\\.jpg")));
    }

    @Test
    void 발급_주소는_경로_방식이고_5분_유효하며_ContentType이_서명에_포함된다() throws Exception {
        String body = presign("project", "image/png", 1).andReturn().getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(body);
        String url = response.get("uploadUrl").asText();

        assertThat(url).startsWith("https://test-account.r2.cloudflarestorage.com/test-bucket/"
                + response.get("key").asText() + "?");
        assertThat(url).contains("X-Amz-Expires=300");
        assertThat(url).containsPattern("X-Amz-SignedHeaders=[^&]*content-type");
        // R2가 지원하지 않는 SDK 기본 체크섬이 붙으면 브라우저 업로드가 실패한다.
        assertThat(url.toLowerCase()).doesNotContain("checksum");
    }

    @Test
    void 키는_요청마다_달라진다() throws Exception {
        String first = presign("project", "image/png", 1).andReturn().getResponse().getContentAsString();
        String second = presign("project", "image/png", 1).andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(first).get("key")).isNotEqualTo(objectMapper.readTree(second).get("key"));
    }

    @Test
    void 허용하지_않는_형식은_INVALID_FILE_TYPE이다() throws Exception {
        presign("project", "image/gif", 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_TYPE"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("contentType"));
    }

    @Test
    void 정확히_5MB는_허용하고_1바이트라도_넘으면_FILE_TOO_LARGE다() throws Exception {
        presign("project", "image/png", 5_242_880).andExpect(status().isOk());

        presign("project", "image/png", 5_242_881)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }

    @Test
    void 필드가_빠졌거나_size가_0_이하면_VALIDATION_FAILED다() throws Exception {
        mockMvc.perform(post("/api/admin/uploads/presign").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));

        presign("project", "image/png", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 없는_target_코드값은_400이다() throws Exception {
        presign("study", "image/png", 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("target"));
    }

    @Test
    void 인증_없이_요청하면_401이다() throws Exception {
        mockMvc.perform(post("/api/admin/uploads/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("project", "image/png", 1)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private ResultActions presign(String target, String contentType, long size) throws Exception {
        return mockMvc.perform(post("/api/admin/uploads/presign").with(user("admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(target, contentType, size)));
    }

    private static String body(String target, String contentType, long size) {
        return """
                { "target": "%s", "contentType": "%s", "size": %d }
                """.formatted(target, contentType, size);
    }
}
