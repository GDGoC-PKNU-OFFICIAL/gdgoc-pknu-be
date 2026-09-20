package com.gdgocpknu.gdgoc_pknu_be.project.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.common.support.PeriodStatus;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.Project;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectCategory;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectResponse;

class ProjectMapperTest {

    /** 2026-09-20 (KST) */
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-09-20T03:00:00Z"), ZoneOffset.UTC);

    private final ProjectMapper mapper = new ProjectMapper(new KstDates(NOW));

    @Test
    void 엔티티를_명세_3_1_모양으로_변환한다() {
        Project project = project("campus-map", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 6, 1));
        project.addTeamMember("홍길동", "백엔드");
        project.addTeamMember("김부경", "프론트엔드");

        ProjectResponse response = mapper.toResponse(project);

        assertThat(response.id()).isEqualTo(12L);
        assertThat(response.slug()).isEqualTo("campus-map");
        assertThat(response.category()).isEqualTo(ProjectCategory.TEAM_PROJECT);
        assertThat(response.period()).isEqualTo(new ProjectResponse.Period("2025.03", "2025.06"));
        assertThat(response.team()).containsExactly(
                new ProjectResponse.TeamMember("홍길동", "백엔드"),
                new ProjectResponse.TeamMember("김부경", "프론트엔드"));
        assertThat(response.techStack()).containsExactly("Next.js", "Spring Boot");
        assertThat(response.links()).isEqualTo(new ProjectResponse.Links("https://github.com/gdgoc-pknu/campus-map", null));
        assertThat(response.thumbnailUrl()).isNull();
    }

    @Test
    void 종료월이_없으면_period_end는_null이고_현재다() {
        ProjectResponse response = mapper.toResponse(project("ongoing", LocalDate.of(2026, 3, 1), null));

        assertThat(response.period().end()).isNull();
        assertThat(response.status()).isEqualTo(PeriodStatus.CURRENT);
    }

    @Test
    void 종료월이_이번_달이면_현재_지난달이면_과거다() {
        assertThat(mapper.toResponse(project("a", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 1))).status())
                .isEqualTo(PeriodStatus.CURRENT);
        assertThat(mapper.toResponse(project("b", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 8, 1))).status())
                .isEqualTo(PeriodStatus.PAST);
    }

    @Test
    void 같은_프로젝트도_달이_바뀌면_과거로_계산된다() {
        Project project = project("a", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 1));
        Clock nextMonth = Clock.fixed(Instant.parse("2026-09-30T15:00:00Z"), ZoneOffset.UTC); // 2026-10-01 00:00 KST

        ProjectResponse response = new ProjectMapper(new KstDates(nextMonth)).toResponse(project);

        assertThat(response.status()).isEqualTo(PeriodStatus.PAST);
    }

    @Test
    void 생성_수정_일시는_KST_오프셋이다() {
        ProjectResponse response = mapper.toResponse(project("a", LocalDate.of(2026, 3, 1), null));

        assertThat(response.createdAt()).isEqualTo(OffsetDateTime.parse("2026-09-15T19:30:00+09:00"));
        assertThat(response.updatedAt()).isEqualTo(OffsetDateTime.parse("2026-09-16T09:00:00+09:00"));
    }

    @Test
    void 목록_변환은_각_항목의_상태를_같은_기준일로_계산한다() {
        List<ProjectResponse> responses = mapper.toResponses(List.of(
                project("past", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 6, 1)),
                project("current", LocalDate.of(2026, 3, 1), null)));

        assertThat(responses).extracting(ProjectResponse::status)
                .containsExactly(PeriodStatus.PAST, PeriodStatus.CURRENT);
    }

    private static Project project(String slug, LocalDate start, LocalDate end) {
        Project project = Project.builder()
                .slug(slug)
                .title("캠퍼스 맵")
                .summary("부경대 길찾기 웹앱")
                .category(ProjectCategory.TEAM_PROJECT)
                .periodStart(start)
                .periodEnd(end)
                .githubUrl("https://github.com/gdgoc-pknu/campus-map")
                .description(List.of("소개 1문단"))
                .techStack(List.of("Next.js", "Spring Boot"))
                .build();
        ReflectionTestUtils.setField(project, "id", 12L);
        ReflectionTestUtils.setField(project, "createdAt", Instant.parse("2026-09-15T10:30:00Z"));
        ReflectionTestUtils.setField(project, "updatedAt", Instant.parse("2026-09-16T00:00:00Z"));
        return project;
    }
}
