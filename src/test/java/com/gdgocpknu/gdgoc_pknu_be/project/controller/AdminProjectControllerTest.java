package com.gdgocpknu.gdgoc_pknu_be.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectRequest;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectRequest.LinksRequest;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectRequest.PeriodRequest;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectRequest.TeamMemberRequest;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectCategory;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectRepository;
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

/** 관리자 CRUD(A-11~13)를 실제 PostgreSQL 위에서 검증한다. 인증 수단이 없는 STEP2 시점이라 `user()`로 인증을 흉내 낸다. */
@IntegrationTest
@Transactional
class AdminProjectControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    EntityManager em;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 등록하면_201과_Location_헤더와_저장된_값을_반환한다() throws Exception {
        mockMvc.perform(post("/api/admin/projects").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("campus-map"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/admin/projects/\\d+")))
                .andExpect(jsonPath("$.slug").value("campus-map"))
                .andExpect(jsonPath("$.category").value("team-project"))
                .andExpect(jsonPath("$.team[0].name").value("홍길동"));
    }

    @Test
    void 인증_없이_등록하면_403이다() throws Exception {
        mockMvc.perform(post("/api/admin/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("no-auth"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void 슬러그가_중복되면_409다() throws Exception {
        mockMvc.perform(post("/api/admin/projects").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request("duplicate-slug"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/projects").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request("duplicate-slug"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLUG_DUPLICATED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("slug"));
    }

    @Test
    void 필드가_빠진_요청은_400이다() throws Exception {
        String bodyWithoutSummary = """
                { "title": "캠퍼스 맵", "slug": "campus-map", "category": "team-project",
                  "period": { "start": "2026.03" }, "description": ["소개"],
                  "team": [], "techStack": [], "features": [], "outcomes": [],
                  "links": {} }
                """;

        mockMvc.perform(post("/api/admin/projects").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(bodyWithoutSummary))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='summary')]").exists());
    }

    @Test
    void 없는_id를_단건_조회하면_404다() throws Exception {
        mockMvc.perform(get("/api/admin/projects/999999").with(user("admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void 수정하면_전체_필드가_교체되고_팀원도_통째로_바뀐다() throws Exception {
        Long id = createAndGetId(request("editable"));

        ProjectRequest update = new ProjectRequest(
                "새 제목", "editable", "새 요약", ProjectCategory.OFFICIAL,
                new PeriodRequest("2026.01", null), null,
                List.of("새 소개"), List.of(new TeamMemberRequest("김부경", "프론트엔드")),
                List.of(), List.of(), List.of(), new LinksRequest(null, null));

        mockMvc.perform(put("/api/admin/projects/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("새 제목"))
                .andExpect(jsonPath("$.category").value("official"))
                .andExpect(jsonPath("$.period.end").doesNotExist())
                .andExpect(jsonPath("$.thumbnailUrl").doesNotExist())
                .andExpect(jsonPath("$.team.length()").value(1))
                .andExpect(jsonPath("$.team[0].name").value("김부경"));

        mockMvc.perform(get("/api/admin/projects/{id}", id).with(user("admin")))
                .andExpect(jsonPath("$.team.length()").value(1))
                .andExpect(jsonPath("$.team[0].name").value("김부경"));
    }

    @Test
    void 수정_시_다른_프로젝트와_슬러그가_겹치면_409다() throws Exception {
        createAndGetId(request("taken-slug"));
        Long id = createAndGetId(request("editable-slug"));

        ProjectRequest update = request("taken-slug");

        mockMvc.perform(put("/api/admin/projects/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLUG_DUPLICATED"));
    }

    @Test
    void 자기_자신의_슬러그로_수정하는_것은_충돌이_아니다() throws Exception {
        Long id = createAndGetId(request("keep-slug"));

        mockMvc.perform(put("/api/admin/projects/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request("keep-slug"))))
                .andExpect(status().isOk());
    }

    @Test
    void 삭제하면_204고_팀원도_함께_삭제된다() throws Exception {
        Long id = createAndGetId(request("deletable"));

        mockMvc.perform(delete("/api/admin/projects/{id}", id).with(user("admin")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/projects/{id}", id).with(user("admin")))
                .andExpect(status().isNotFound());
        assertThat(projectRepository.existsById(id)).isFalse();
    }

    @Test
    void 없는_id를_삭제하면_404다() throws Exception {
        mockMvc.perform(delete("/api/admin/projects/999999").with(user("admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void 슬러그_중복_확인은_존재_여부와_excludeId를_반영한다() throws Exception {
        Long id = createAndGetId(request("check-me"));

        mockMvc.perform(get("/api/admin/projects/slug-check").with(user("admin")).param("slug", "check-me"))
                .andExpect(jsonPath("$.available").value(false));

        mockMvc.perform(get("/api/admin/projects/slug-check").with(user("admin"))
                        .param("slug", "check-me").param("excludeId", String.valueOf(id)))
                .andExpect(jsonPath("$.available").value(true));

        mockMvc.perform(get("/api/admin/projects/slug-check").with(user("admin")).param("slug", "unused-slug"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void 목록_조회는_페이지_모양으로_반환한다() throws Exception {
        createAndGetId(request("page-a"));
        createAndGetId(request("page-b"));

        mockMvc.perform(get("/api/admin/projects").with(user("admin")).param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void 수정_응답의_updatedAt은_수정_후_값이다() throws Exception {
        Long id = createAndGetId(request("touch-me"));
        em.flush();
        jdbc.update("UPDATE project SET updated_at = TIMESTAMPTZ '2020-01-01 00:00:00+09' WHERE id = ?", id);
        em.clear();

        ProjectRequest update = new ProjectRequest(
                "바뀐 제목", "touch-me", "부경대 길찾기 웹앱", ProjectCategory.TEAM_PROJECT,
                new PeriodRequest("2026.03", "2026.06"), null,
                List.of("소개 문단"), List.of(), List.of(), List.of(), List.of(), new LinksRequest(null, null));

        mockMvc.perform(put("/api/admin/projects/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.startsWith("2020-"))));
    }

    @Test
    void 팀원_배열에_null_원소가_있으면_500이_아니라_400이다() throws Exception {
        mockMvc.perform(post("/api/admin/projects").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(rawBody("null-team", "[null]", "[\"소개\"]", "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='team[0]')]").exists());
    }

    @Test
    void 문자열_배열에_빈_값이나_null_원소가_있으면_400이다() throws Exception {
        mockMvc.perform(post("/api/admin/projects").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawBody("blank-items", "[]", "[\"\", null]", "[\"  \"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='description[0]')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='description[1]')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='techStack[0]')]").exists());
    }

    /** 원소 단위로 잘못된 배열을 보내기 위해 JSON을 직접 만든다(record로는 null 원소 리스트를 만들기 번거롭다). */
    private static String rawBody(String slug, String team, String description, String techStack) {
        return """
                { "title": "캠퍼스 맵", "slug": "%s", "summary": "요약", "category": "official",
                  "period": { "start": "2026.03", "end": null }, "thumbnailUrl": null,
                  "description": %s, "team": %s, "techStack": %s, "features": [], "outcomes": [],
                  "links": { "github": null, "demo": null } }
                """.formatted(slug, description, team, techStack);
    }

    private Long createAndGetId(ProjectRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/projects").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    private String json(ProjectRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private static ProjectRequest request(String slug) {
        return new ProjectRequest(
                "캠퍼스 맵", slug, "부경대 길찾기 웹앱", ProjectCategory.TEAM_PROJECT,
                new PeriodRequest("2026.03", "2026.06"), null,
                List.of("소개 문단"), List.of(new TeamMemberRequest("홍길동", "백엔드")),
                List.of("Next.js", "Spring Boot"), List.of(), List.of(),
                new LinksRequest("https://github.com/gdgoc-pknu/" + slug, null));
    }
}
