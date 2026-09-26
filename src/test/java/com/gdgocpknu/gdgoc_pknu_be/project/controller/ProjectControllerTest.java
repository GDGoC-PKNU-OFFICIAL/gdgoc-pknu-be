package com.gdgocpknu.gdgoc_pknu_be.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.Project;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectCategory;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectRepository;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;

/**
 * 실제 PostgreSQL을 통과하는 공개 조회 검증. 저장 후 영속성 컨텍스트를 비워서
 * jsonb · 컨버터 · 정렬 어노테이션이 DB 왕복에서 제대로 동작하는지 본다.
 */
@IntegrationTest
@Transactional
class ProjectControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    EntityManager em;
    @Autowired
    Clock clock;

    @Test
    void 인증_없이_전체_목록을_시작월_최신순으로_반환한다() throws Exception {
        LocalDate thisMonth = LocalDate.now(clock).withDayOfMonth(1);
        save("old", thisMonth.minusMonths(20), thisMonth.minusMonths(17));
        save("newest", thisMonth, null);
        save("middle", thisMonth.minusMonths(8), thisMonth.minusMonths(5));
        flushAndClear();

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].slug").value("newest"))
                .andExpect(jsonPath("$[1].slug").value("middle"))
                .andExpect(jsonPath("$[2].slug").value("old"));
    }

    @Test
    void 종료월로_status를_계산해_내려준다() throws Exception {
        LocalDate thisMonth = LocalDate.now(clock).withDayOfMonth(1);
        save("ongoing", thisMonth.minusMonths(2), null);
        save("ends-this-month", thisMonth.minusMonths(3), thisMonth);
        save("finished", thisMonth.minusMonths(9), thisMonth.minusMonths(1));
        flushAndClear();

        mockMvc.perform(get("/api/projects"))
                .andExpect(jsonPath("$[?(@.slug=='ongoing')].status").value("current"))
                .andExpect(jsonPath("$[?(@.slug=='ends-this-month')].status").value("current"))
                .andExpect(jsonPath("$[?(@.slug=='finished')].status").value("past"));
    }

    @Test
    void 명세_3_1의_필드_모양으로_직렬화된다() throws Exception {
        Project project = Project.builder()
                .slug("campus-map").title("캠퍼스 맵").summary("길찾기 웹앱")
                .category(ProjectCategory.TEAM_PROJECT)
                .periodStart(LocalDate.of(2025, 3, 1)).periodEnd(LocalDate.of(2025, 6, 1))
                .githubUrl("https://github.com/gdgoc-pknu/campus-map")
                .description(List.of("첫 문단", "둘째 문단"))
                .techStack(List.of("Next.js", "Spring Boot"))
                .build();
        project.addTeamMember("홍길동", "백엔드");
        project.addTeamMember("김부경", "프론트엔드");
        project.addTeamMember("이서울", "디자인");
        projectRepository.save(project);
        flushAndClear();

        mockMvc.perform(get("/api/projects"))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].category").value("team-project"))
                .andExpect(jsonPath("$[0].period.start").value("2025.03"))
                .andExpect(jsonPath("$[0].period.end").value("2025.06"))
                .andExpect(jsonPath("$[0].description[0]").value("첫 문단"))
                .andExpect(jsonPath("$[0].description[1]").value("둘째 문단"))
                .andExpect(jsonPath("$[0].techStack[1]").value("Spring Boot"))
                .andExpect(jsonPath("$[0].team[0].name").value("홍길동"))
                .andExpect(jsonPath("$[0].team[1].role").value("프론트엔드"))
                .andExpect(jsonPath("$[0].team[2].name").value("이서울"))
                .andExpect(jsonPath("$[0].links.github").value("https://github.com/gdgoc-pknu/campus-map"))
                .andExpect(jsonPath("$[0].createdAt").value(endsWith("+09:00")))
                .andExpect(jsonPath("$[0].updatedAt").value(endsWith("+09:00")));
    }

    @Test
    void 값이_없는_선택_필드는_생략하고_배열은_빈_배열로_내려준다() throws Exception {
        save("minimal", LocalDate.of(2026, 3, 1), null);
        flushAndClear();

        mockMvc.perform(get("/api/projects"))
                .andExpect(jsonPath("$[0].thumbnailUrl").doesNotExist())
                .andExpect(jsonPath("$[0].period.end").doesNotExist())
                .andExpect(jsonPath("$[0].links.github").doesNotExist())
                .andExpect(jsonPath("$[0].links.demo").doesNotExist())
                .andExpect(jsonPath("$[0].team").isEmpty())
                .andExpect(jsonPath("$[0].features").isEmpty())
                .andExpect(jsonPath("$[0].outcomes").isEmpty());
    }

    @Test
    void 프로젝트가_없으면_빈_배열이다() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void 목록_조회_쿼리_수는_프로젝트_수에_비례하지_않는다() throws Exception {
        LocalDate start = LocalDate.of(2025, 3, 1);
        for (int i = 0; i < 8; i++) {
            Project project = project("project-" + i, start.plusMonths(i), null);
            project.addTeamMember("팀원A" + i, "백엔드");
            project.addTeamMember("팀원B" + i, "프론트엔드");
            projectRepository.save(project);
        }
        flushAndClear();
        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/projects")).andExpect(jsonPath("$.length()").value(8));

        // 프로젝트 목록 1회 + 팀원 batch fetch 1회. N+1이면 8건에 9회 이상이 된다.
        // 하한(1)은 통계가 꺼져 있어 0으로 "통과"하는 경우를 잡는다.
        assertThat(statistics.getPrepareStatementCount()).isBetween(1L, 2L);
    }

    private void save(String slug, LocalDate start, LocalDate end) {
        projectRepository.save(project(slug, start, end));
    }

    private static Project project(String slug, LocalDate start, LocalDate end) {
        return Project.builder()
                .slug(slug).title("제목").summary("요약")
                .category(ProjectCategory.OFFICIAL)
                .periodStart(start).periodEnd(end)
                .build();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
