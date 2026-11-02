package com.gdgocpknu.gdgoc_pknu_be.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRepository;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectRepository;
import com.gdgocpknu.gdgoc_pknu_be.support.FakeS3Client;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 프로젝트 · 멤버 저장 경로가 재검증을 실제로 거치는지 확인한다(명세 2-4, 2-7, 5-7). */
@IntegrationTest
@Transactional
class UploadVerificationIntegrationTest {

    private static final String BASE = "https://test-bucket.r2.dev/";
    private static final String GOOD = "8f1c0000-0000-4000-8000-000000000001.webp";
    private static final String BAD = "8f1c0000-0000-4000-8000-000000000002.webp";
    private static final long NINE_MB = 9_437_184;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    FakeS3Client s3;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        s3.reset();
        for (String prefix : new String[] {"projects/", "members/"}) {
            s3.put(prefix + GOOD, 348_120, "image/webp");
            s3.put(prefix + BAD, NINE_MB, "image/webp");
        }
    }

    @Test
    void 프로젝트_등록_정상_썸네일은_저장된다() throws Exception {
        save(post("/api/admin/projects"), project("ok", thumb(GOOD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.thumbnailUrl").value(BASE + "projects/" + GOOD));
    }

    @Test
    void 프로젝트_등록_5MB_초과_썸네일은_400이고_저장되지_않으며_객체가_삭제된다() throws Exception {
        long before = projectRepository.count();

        save(post("/api/admin/projects"), project("too-large", thumb(BAD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UPLOAD_VERIFICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("업로드한 이미지를 확인할 수 없습니다. 다시 올려 주세요."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("thumbnailUrl"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("5MB 이하 파일만 올릴 수 있습니다."));

        assertThat(projectRepository.count()).isEqualTo(before);
        assertThat(s3.exists("projects/" + BAD)).isFalse();
    }

    @Test
    void 슬러그가_중복이면_재검증_전에_409로_막아_정상_객체를_건드리지_않는다() throws Exception {
        save(post("/api/admin/projects"), project("dup", null)).andExpect(status().isCreated());

        save(post("/api/admin/projects"), project("dup", thumb(BAD)))
                .andExpect(status().isConflict());

        assertThat(s3.exists("projects/" + BAD)).isTrue();
    }

    @Test
    void 프로젝트_수정_썸네일이_그대로면_재검증하지_않는다() throws Exception {
        long id = create(post("/api/admin/projects"), project("keep", thumb(GOOD)));
        s3.reset(); // 객체가 사라져도 값이 그대로면 R2를 조회하지 않아야 한다

        save(put("/api/admin/projects/" + id), project("keep", thumb(GOOD)))
                .andExpect(status().isOk());
    }

    @Test
    void 프로젝트_수정_새_썸네일이_초과면_400이고_이전_값을_유지한다() throws Exception {
        long id = create(post("/api/admin/projects"), project("change", thumb(GOOD)));

        save(put("/api/admin/projects/" + id), project("change", thumb(BAD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("thumbnailUrl"));

        assertThat(projectRepository.findById(id).orElseThrow().getThumbnailUrl()).isEqualTo(BASE + "projects/" + GOOD);
    }

    @Test
    void 프로젝트_수정_썸네일을_비우면_재검증_없이_제거된다() throws Exception {
        long id = create(post("/api/admin/projects"), project("clear", thumb(GOOD)));
        s3.reset();

        save(put("/api/admin/projects/" + id), project("clear", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thumbnailUrl").doesNotExist());
    }

    @Test
    void 프로젝트에_멤버_접두사_URL을_넣으면_400이고_그_객체는_남는다() throws Exception {
        save(post("/api/admin/projects"), project("wrong-prefix", BASE + "members/" + BAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UPLOAD_VERIFICATION_FAILED"));

        assertThat(s3.exists("members/" + BAD)).isTrue();
    }

    @Test
    void 멤버_등록_정상_사진은_저장되고_없는_객체는_400이다() throws Exception {
        save(post("/api/admin/members"), member(photo(GOOD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoUrl").value(BASE + "members/" + GOOD));

        long before = memberRepository.count();
        save(post("/api/admin/members"), member(BASE + "members/8f1c0000-0000-4000-8000-000000000003.webp"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UPLOAD_VERIFICATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("photoUrl"));
        assertThat(memberRepository.count()).isEqualTo(before);
    }

    @Test
    void 멤버_수정_같은_사진은_통과하고_새_사진이_초과면_400이다() throws Exception {
        long id = create(post("/api/admin/members"), member(photo(GOOD)));
        s3.reset();
        save(put("/api/admin/members/" + id), member(photo(GOOD))).andExpect(status().isOk());

        s3.put("members/" + BAD, NINE_MB, "image/webp");
        save(put("/api/admin/members/" + id), member(photo(BAD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("photoUrl"));

        assertThat(memberRepository.findById(id).orElseThrow().getPhotoUrl()).isEqualTo(BASE + "members/" + GOOD);
        assertThat(s3.exists("members/" + BAD)).isFalse();
    }

    private static String thumb(String name) {
        return BASE + "projects/" + name;
    }

    private static String photo(String name) {
        return BASE + "members/" + name;
    }

    private long create(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String body)
            throws Exception {
        String response = save(request, body)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private ResultActions save(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String body)
            throws Exception {
        return mockMvc.perform(request.with(user("admin")).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static String project(String slug, String thumbnailUrl) {
        return """
                { "title": "캠퍼스 맵", "slug": "%s", "summary": "요약", "category": "team-project",
                  "period": { "start": "2026.03", "end": null }, "thumbnailUrl": %s,
                  "description": ["소개"], "team": [], "techStack": [], "features": [], "outcomes": [],
                  "links": {} }
                """.formatted(slug, quoted(thumbnailUrl));
    }

    private static String member(String photoUrl) {
        return """
                { "status": "senior", "role": null, "name": "이시니", "photoUrl": %s, "department": "컴퓨터공학과",
                  "history": [], "interests": [], "part": "backend", "bio": "소개", "links": {},
                  "consentConfirmed": true }
                """.formatted(quoted(photoUrl));
    }
}
