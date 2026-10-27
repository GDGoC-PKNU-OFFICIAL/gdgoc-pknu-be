package com.gdgocpknu.gdgoc_pknu_be.study.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgocpknu.gdgoc_pknu_be.study.domain.StudyRepository;
import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudyRequest;
import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudyRequest.OutputRequest;
import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudyRequest.PeriodRequest;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 CRUD(A-22, A-23)를 실제 PostgreSQL 위에서 검증한다. 목록 필터 · 정렬은 AdminStudyListTest가 맡는다. */
@IntegrationTest
@Transactional
class AdminStudyControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    StudyRepository studyRepository;
    @Autowired
    EntityManager em;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 등록하면_201과_Location_헤더와_저장된_값을_반환한다() throws Exception {
        mockMvc.perform(post("/api/admin/studies").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("spring-study"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/admin/studies/\\d+")))
                .andExpect(jsonPath("$.slug").value("spring-study"))
                .andExpect(jsonPath("$.status").value("past"))
                .andExpect(jsonPath("$.participants[0]").value("김부경"))
                .andExpect(jsonPath("$.participants[1]").value("이서울"))
                .andExpect(jsonPath("$.outputs[0].label").value("회고록"));
    }

    @Test
    void 인증_없이_등록하면_401이다() throws Exception {
        mockMvc.perform(post("/api/admin/studies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("no-auth"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 슬러그가_중복되면_409다() throws Exception {
        createAndGetId(request("duplicate-slug"));

        mockMvc.perform(post("/api/admin/studies").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request("duplicate-slug"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLUG_DUPLICATED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("slug"));
    }

    @Test
    void 필드가_빠진_요청은_400이다() throws Exception {
        String bodyWithoutSummary = """
                { "title": "스프링 스터디", "slug": "spring-study", "topic": "Backend",
                  "period": { "start": "2026.03" }, "leader": "홍길동",
                  "participants": [], "outputs": [] }
                """;

        mockMvc.perform(post("/api/admin/studies").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(bodyWithoutSummary))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='summary')]").exists());
    }

    @Test
    void 없는_id를_단건_조회하면_404다() throws Exception {
        mockMvc.perform(get("/api/admin/studies/999999").with(user("admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void 수정하면_전체_필드가_교체되고_참여자_산출물도_통째로_바뀐다() throws Exception {
        Long id = createAndGetId(request("editable"));

        StudyRequest update = new StudyRequest(
                "새 제목", "editable", "새 요약", "AI",
                new PeriodRequest("2026.01", null), "박리더",
                List.of("최참여"), null,
                List.of(new OutputRequest("새 자료", "https://example.com/new")));

        mockMvc.perform(put("/api/admin/studies/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("새 제목"))
                .andExpect(jsonPath("$.topic").value("AI"))
                .andExpect(jsonPath("$.leader").value("박리더"))
                .andExpect(jsonPath("$.period.end").doesNotExist())
                .andExpect(jsonPath("$.format").doesNotExist())
                .andExpect(jsonPath("$.participants.length()").value(1))
                .andExpect(jsonPath("$.participants[0]").value("최참여"))
                .andExpect(jsonPath("$.outputs.length()").value(1))
                .andExpect(jsonPath("$.outputs[0].label").value("새 자료"));

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/admin/studies/{id}", id).with(user("admin")))
                .andExpect(jsonPath("$.participants.length()").value(1))
                .andExpect(jsonPath("$.participants[0]").value("최참여"))
                .andExpect(jsonPath("$.outputs.length()").value(1));
    }

    @Test
    void 빈_배열로_수정하면_참여자_산출물이_모두_사라진다() throws Exception {
        Long id = createAndGetId(request("to-empty"));

        StudyRequest update = new StudyRequest(
                "스프링 스터디", "to-empty", "요약", "Backend",
                new PeriodRequest("2026.03", "2026.06"), "홍길동", List.of(), null, List.of());

        mockMvc.perform(put("/api/admin/studies/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants").isEmpty())
                .andExpect(jsonPath("$.outputs").isEmpty());

        em.flush();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM study_participant WHERE study_id = ?", Integer.class, id))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM study_output WHERE study_id = ?", Integer.class, id))
                .isZero();
    }

    @Test
    void 수정_요청에서_필드가_빠지면_400이고_기존_값은_바뀌지_않는다() throws Exception {
        Long id = createAndGetId(request("put-missing"));
        // PUT은 전체 필드 교체라 "안 보낸 필드는 그대로 둔다"로 해석하면 안 된다 (명세 1-4). participants · outputs 누락.
        String bodyWithoutArrays = """
                { "title": "바뀐 제목", "slug": "put-missing", "summary": "요약", "topic": "Backend",
                  "period": { "start": "2026.03", "end": null }, "leader": "홍길동", "format": null }
                """;

        mockMvc.perform(put("/api/admin/studies/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(bodyWithoutArrays))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='participants')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='outputs')]").exists());

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/admin/studies/{id}", id).with(user("admin")))
                .andExpect(jsonPath("$.title").value("스프링 스터디"))
                .andExpect(jsonPath("$.participants[0]").value("김부경"));
    }

    @Test
    void 수정_시_다른_스터디와_슬러그가_겹치면_409다() throws Exception {
        createAndGetId(request("taken-slug"));
        Long id = createAndGetId(request("editable-slug"));

        mockMvc.perform(put("/api/admin/studies/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request("taken-slug"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLUG_DUPLICATED"));
    }

    @Test
    void 자기_자신의_슬러그로_수정하는_것은_충돌이_아니다() throws Exception {
        Long id = createAndGetId(request("keep-slug"));

        mockMvc.perform(put("/api/admin/studies/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request("keep-slug"))))
                .andExpect(status().isOk());
    }

    @Test
    void 삭제하면_204고_참여자_산출물도_함께_삭제된다() throws Exception {
        Long id = createAndGetId(request("deletable"));
        em.flush();
        em.clear();

        mockMvc.perform(delete("/api/admin/studies/{id}", id).with(user("admin")))
                .andExpect(status().isNoContent());
        em.flush();

        mockMvc.perform(get("/api/admin/studies/{id}", id).with(user("admin")))
                .andExpect(status().isNotFound());
        assertThat(studyRepository.existsById(id)).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM study_participant WHERE study_id = ?", Integer.class, id))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM study_output WHERE study_id = ?", Integer.class, id))
                .isZero();
    }

    @Test
    void 없는_id를_삭제하면_404다() throws Exception {
        mockMvc.perform(delete("/api/admin/studies/999999").with(user("admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void 슬러그_중복_확인은_존재_여부와_excludeId를_반영한다() throws Exception {
        Long id = createAndGetId(request("check-me"));

        mockMvc.perform(get("/api/admin/studies/slug-check").with(user("admin")).param("slug", "check-me"))
                .andExpect(jsonPath("$.available").value(false));

        mockMvc.perform(get("/api/admin/studies/slug-check").with(user("admin"))
                        .param("slug", "check-me").param("excludeId", String.valueOf(id)))
                .andExpect(jsonPath("$.available").value(true));

        mockMvc.perform(get("/api/admin/studies/slug-check").with(user("admin")).param("slug", "unused-slug"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void 수정_응답의_updatedAt은_수정_후_값이다() throws Exception {
        Long id = createAndGetId(request("touch-me"));
        em.flush();
        jdbc.update("UPDATE study SET updated_at = TIMESTAMPTZ '2020-01-01 00:00:00+09' WHERE id = ?", id);
        em.clear();

        StudyRequest update = new StudyRequest(
                "바뀐 제목", "touch-me", "스프링 부트 기초부터 JPA까지", "Backend",
                new PeriodRequest("2026.03", "2026.06"), "홍길동", List.of(), null, List.of());

        mockMvc.perform(put("/api/admin/studies/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value(not(startsWith("2020-"))));
    }

    @Test
    void 참여자만_바꿔도_updatedAt이_갱신된다() throws Exception {
        Long id = createAndGetId(request("children-only"));
        em.flush();
        jdbc.update("UPDATE study SET updated_at = TIMESTAMPTZ '2020-01-01 00:00:00+09' WHERE id = ?", id);
        em.clear();

        StudyRequest update = new StudyRequest(
                "스프링 스터디", "children-only", "스프링 부트 기초부터 JPA까지", "Backend",
                new PeriodRequest("2026.03", "2026.06"), "홍길동",
                List.of("새참여자"), "주 1회 온라인",
                List.of(new OutputRequest("회고록", "https://velog.io/@gdgoc/children-only")));

        mockMvc.perform(put("/api/admin/studies/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants[0]").value("새참여자"))
                .andExpect(jsonPath("$.updatedAt").value(not(startsWith("2020-"))));
    }

    @Test
    void 참여자_배열에_빈_값이나_null_원소가_있으면_500이_아니라_400이다() throws Exception {
        mockMvc.perform(post("/api/admin/studies").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawBody("blank-items", "[\"\", null]", "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='participants[0]')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='participants[1]')]").exists());
    }

    @Test
    void 산출물_배열에_null_원소가_있으면_500이_아니라_400이다() throws Exception {
        mockMvc.perform(post("/api/admin/studies").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawBody("null-output", "[]", "[null]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='outputs[0]')]").exists());
    }

    @Test
    void 문자열은_앞뒤_공백을_제거한_뒤_검사하고_저장한다() throws Exception {
        String paddedTitle = "  " + "가".repeat(40) + "　 ";   // 공백 제외 40자 = 최대 길이
        StudyRequest request = new StudyRequest(
                paddedTitle, " trimmed-slug ", " 요약 ", " Backend ",
                new PeriodRequest(" 2026.03 ", null), " 홍길동 ",
                List.of(" 김부경 "), " 주 1회 ",
                List.of(new OutputRequest(" 회고록 ", " https://velog.io/@gdgoc ")));

        mockMvc.perform(post("/api/admin/studies").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("가".repeat(40)))
                .andExpect(jsonPath("$.slug").value("trimmed-slug"))
                .andExpect(jsonPath("$.period.start").value("2026.03"))
                .andExpect(jsonPath("$.leader").value("홍길동"))
                .andExpect(jsonPath("$.participants[0]").value("김부경"))
                .andExpect(jsonPath("$.format").value("주 1회"))
                .andExpect(jsonPath("$.outputs[0].url").value("https://velog.io/@gdgoc"));
    }

    @Test
    void format을_빈_문자열이나_공백으로_보내면_null로_저장되고_응답에서_생략된다() throws Exception {
        StudyRequest blank = new StudyRequest(
                "스프링 스터디", "blank-format", "요약", "Backend",
                new PeriodRequest("2026.03", null), "홍길동", List.of(), "   ", List.of());
        Long id = createAndGetId(blank);

        StudyRequest empty = new StudyRequest(
                "스프링 스터디", "blank-format", "요약", "Backend",
                new PeriodRequest("2026.03", null), "홍길동", List.of(), "", List.of());
        mockMvc.perform(put("/api/admin/studies/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(empty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").doesNotExist());

        em.flush();
        assertThat(jdbc.queryForObject("SELECT format FROM study WHERE id = ?", String.class, id)).isNull();
    }

    /** 원소 단위로 잘못된 배열을 보내기 위해 JSON을 직접 만든다(record로는 null 원소 리스트를 만들기 번거롭다). */
    private static String rawBody(String slug, String participants, String outputs) {
        return """
                { "title": "스프링 스터디", "slug": "%s", "summary": "요약", "topic": "Backend",
                  "period": { "start": "2026.03", "end": null }, "leader": "홍길동",
                  "participants": %s, "format": null, "outputs": %s }
                """.formatted(slug, participants, outputs);
    }

    private Long createAndGetId(StudyRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/studies").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    private String json(StudyRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private static StudyRequest request(String slug) {
        return new StudyRequest(
                "스프링 스터디", slug, "스프링 부트 기초부터 JPA까지", "Backend",
                new PeriodRequest("2026.03", "2026.06"), "홍길동",
                List.of("김부경", "이서울"), "주 1회 온라인",
                List.of(new OutputRequest("회고록", "https://velog.io/@gdgoc/" + slug)));
    }
}
