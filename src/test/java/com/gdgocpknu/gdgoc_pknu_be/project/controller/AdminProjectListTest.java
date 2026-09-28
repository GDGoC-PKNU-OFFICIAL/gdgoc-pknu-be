package com.gdgocpknu.gdgoc_pknu_be.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gdgocpknu.gdgoc_pknu_be.project.domain.Project;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectCategory;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectRepository;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 목록(A-11)의 필터 · 정렬 · 잘못된 파라미터 · 쿼리 수를 실제 PostgreSQL 위에서 검증한다 (API 명세서 1-6, 2-4).
 * CRUD 자체는 AdminProjectControllerTest가 맡는다.
 */
@IntegrationTest
@Transactional
class AdminProjectListTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    EntityManager em;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    Clock clock;

    // ───────────────────────────── 필터 ─────────────────────────────

    @Test
    void q는_제목을_대소문자_구분_없이_부분_일치로_찾는다() throws Exception {
        save(project("campus-map").title("Campus 맵"));
        save(project("pknu-bot").title("부경 챗봇"));
        flushAndClear();

        mockMvc.perform(list().param("q", "campus"))
                .andExpect(jsonPath("$.items[*].slug", contains("campus-map")))
                .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(list().param("q", "  챗봇 "))
                .andExpect(jsonPath("$.items[*].slug", contains("pknu-bot")));
    }

    @Test
    void q의_퍼센트와_밑줄은_와일드카드가_아니라_글자로_찾는다() throws Exception {
        save(project("rate").title("달성률 100% 대시보드"));
        save(project("plain").title("달성률 1000 대시보드"));
        flushAndClear();

        mockMvc.perform(list().param("q", "100%"))
                .andExpect(jsonPath("$.items[*].slug", contains("rate")));
        mockMvc.perform(list().param("q", "_"))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void 빈_q는_검색하지_않는다() throws Exception {
        save(project("a"));
        save(project("b"));
        flushAndClear();

        mockMvc.perform(list().param("q", "   "))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void status는_이번_달_기준으로_계산한_값으로_거른다() throws Exception {
        LocalDate thisMonth = LocalDate.now(clock).withDayOfMonth(1);
        save(project("ongoing").periodStart(thisMonth.minusMonths(2)).periodEnd(null));
        save(project("ends-this-month").periodStart(thisMonth.minusMonths(3)).periodEnd(thisMonth));
        save(project("finished").periodStart(thisMonth.minusMonths(9)).periodEnd(thisMonth.minusMonths(1)));
        flushAndClear();

        mockMvc.perform(list().param("status", "current"))
                .andExpect(jsonPath("$.items[*].slug", containsInAnyOrder("ongoing", "ends-this-month")))
                .andExpect(jsonPath("$.items[*].status", contains("current", "current")));

        mockMvc.perform(list().param("status", "past"))
                .andExpect(jsonPath("$.items[*].slug", contains("finished")))
                .andExpect(jsonPath("$.items[0].status").value("past"));
    }

    @Test
    void category는_코드값으로_거른다() throws Exception {
        save(project("sc").category(ProjectCategory.SOLUTION_CHALLENGE));
        save(project("team").category(ProjectCategory.TEAM_PROJECT));
        flushAndClear();

        mockMvc.perform(list().param("category", "solution-challenge"))
                .andExpect(jsonPath("$.items[*].slug", contains("sc")));
    }

    @Test
    void year는_시작월의_연도로_거르고_경계_월을_포함한다() throws Exception {
        save(project("jan-2025").periodStart(LocalDate.of(2025, 1, 1)));
        save(project("dec-2025").periodStart(LocalDate.of(2025, 12, 1)));
        save(project("dec-2024").periodStart(LocalDate.of(2024, 12, 1)));
        save(project("jan-2026").periodStart(LocalDate.of(2026, 1, 1)));
        flushAndClear();

        mockMvc.perform(list().param("year", "2025"))
                .andExpect(jsonPath("$.items[*].slug", contains("dec-2025", "jan-2025")));
    }

    @Test
    void 여러_조건은_AND로_결합한다() throws Exception {
        save(project("match").title("캠퍼스 맵").category(ProjectCategory.OFFICIAL)
                .periodStart(LocalDate.of(2025, 3, 1)).periodEnd(LocalDate.of(2025, 6, 1)));
        save(project("other-year").title("캠퍼스 맵").category(ProjectCategory.OFFICIAL)
                .periodStart(LocalDate.of(2024, 3, 1)).periodEnd(LocalDate.of(2024, 6, 1)));
        save(project("other-category").title("캠퍼스 맵").category(ProjectCategory.TEAM_PROJECT)
                .periodStart(LocalDate.of(2025, 3, 1)).periodEnd(LocalDate.of(2025, 6, 1)));
        save(project("other-title").title("챗봇").category(ProjectCategory.OFFICIAL)
                .periodStart(LocalDate.of(2025, 3, 1)).periodEnd(LocalDate.of(2025, 6, 1)));
        flushAndClear();

        mockMvc.perform(list().param("q", "캠퍼스").param("category", "official")
                        .param("year", "2025").param("status", "past"))
                .andExpect(jsonPath("$.items[*].slug", contains("match")));
    }

    // ───────────────────────────── 정렬 ─────────────────────────────

    @Test
    void 기본_정렬은_시작월_최신순이다() throws Exception {
        save(project("old").periodStart(LocalDate.of(2024, 3, 1)));
        save(project("new").periodStart(LocalDate.of(2026, 3, 1)));
        save(project("mid").periodStart(LocalDate.of(2025, 3, 1)));
        flushAndClear();

        mockMvc.perform(list())
                .andExpect(jsonPath("$.items[*].slug", contains("new", "mid", "old")));
    }

    @Test
    void sort로_제목_오름차순_정렬한다() throws Exception {
        save(project("c").title("다"));
        save(project("a").title("가"));
        save(project("b").title("나"));
        flushAndClear();

        mockMvc.perform(list().param("sort", "title,asc"))
                .andExpect(jsonPath("$.items[*].slug", contains("a", "b", "c")));
    }

    @Test
    void sort로_최근_수정순_정렬한다() throws Exception {
        Long first = save(project("first"));
        Long second = save(project("second"));
        flushAndClear();
        jdbc.update("UPDATE project SET updated_at = TIMESTAMPTZ '2026-01-01 00:00:00+09' WHERE id = ?", first);
        jdbc.update("UPDATE project SET updated_at = TIMESTAMPTZ '2025-01-01 00:00:00+09' WHERE id = ?", second);

        mockMvc.perform(list().param("sort", "updatedAt,desc"))
                .andExpect(jsonPath("$.items[*].slug", contains("first", "second")));
    }

    @Test
    void 정렬_값이_같으면_id_역순으로_고정되어_페이지가_겹치지_않는다() throws Exception {
        LocalDate sameMonth = LocalDate.of(2026, 3, 1);
        save(project("p1").periodStart(sameMonth));
        save(project("p2").periodStart(sameMonth));
        save(project("p3").periodStart(sameMonth));
        flushAndClear();

        mockMvc.perform(list().param("size", "2").param("page", "0"))
                .andExpect(jsonPath("$.items[*].slug", contains("p3", "p2")));
        mockMvc.perform(list().param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.items[*].slug", contains("p1")));
    }

    // ───────────────────────── 잘못된 파라미터 ─────────────────────────

    @Test
    void 허용하지_않는_정렬_필드는_400이다() throws Exception {
        mockMvc.perform(list().param("sort", "slug,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"));
    }

    @Test
    void 정의되지_않은_status_코드값은_400이다() throws Exception {
        mockMvc.perform(list().param("status", "done"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
    }

    @Test
    void 정의되지_않은_category_코드값은_400이다() throws Exception {
        mockMvc.perform(list().param("category", "hackathon"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("category"));
    }

    @Test
    void 숫자가_아니거나_범위를_벗어난_year는_400이다() throws Exception {
        mockMvc.perform(list().param("year", "2025년"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("year"));

        mockMvc.perform(list().param("year", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("year"));
    }

    // ───────────────────────────── N+1 ─────────────────────────────

    @Test
    void 목록_조회_쿼리_수는_프로젝트_수에_비례하지_않는다() throws Exception {
        LocalDate start = LocalDate.of(2024, 1, 1);
        for (int i = 0; i < 25; i++) {
            Project project = project("project-" + i).title("프로젝트 " + i).periodStart(start.plusMonths(i)).build();
            project.addTeamMember("팀원A" + i, "백엔드");
            project.addTeamMember("팀원B" + i, "프론트엔드");
            projectRepository.save(project);
        }
        flushAndClear();
        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(list().param("size", "20").param("q", "프로젝트").param("status", "current"))
                .andExpect(jsonPath("$.items.length()").value(20))
                .andExpect(jsonPath("$.items[0].team.length()").value(2))
                .andExpect(jsonPath("$.totalItems").value(25));

        // 목록 1회 + 전체 건수(count) 1회 + 팀원 batch fetch 1회. N+1이면 20건에 22회 이상이 된다.
        // 하한(1)은 통계가 꺼져 있어 0으로 "통과"하는 경우를 잡는다.
        assertThat(statistics.getPrepareStatementCount()).isBetween(1L, 3L);
    }

    // ───────────────────────────── 도우미 ─────────────────────────────

    private static MockHttpServletRequestBuilder list() {
        return get("/api/admin/projects").with(user("admin"));
    }

    private Long save(Project.ProjectBuilder builder) {
        return projectRepository.save(builder.build()).getId();
    }

    private static Project.ProjectBuilder project(String slug) {
        return Project.builder()
                .slug(slug).title("제목").summary("요약")
                .category(ProjectCategory.OFFICIAL)
                .periodStart(LocalDate.of(2026, 3, 1));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
