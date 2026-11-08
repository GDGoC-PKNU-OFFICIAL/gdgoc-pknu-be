package com.gdgocpknu.gdgoc_pknu_be.dashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.Event;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventCategory;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventRepository;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.Member;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberPart;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRepository;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRole;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.Project;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectCategory;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectRepository;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSettingRepository;
import com.gdgocpknu.gdgoc_pknu_be.study.domain.Study;
import com.gdgocpknu.gdgoc_pknu_be.study.domain.StudyRepository;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 대시보드 요약(A-00). 날짜는 오늘(KST) 기준 상대값으로 만들어 실행 시점과 무관하게 한다. */
@IntegrationTest
@Transactional
class AdminDashboardControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    EntityManager em;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    KstDates kstDates;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    StudyRepository studyRepository;
    @Autowired
    EventRepository eventRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    SiteSettingRepository siteSettingRepository;

    private LocalDate today;
    private LocalDate thisMonth;

    @BeforeEach
    void clean() {
        projectRepository.deleteAll();
        studyRepository.deleteAll();
        eventRepository.deleteAll();
        memberRepository.deleteAll();
        siteSettingRepository.deleteAll();
        flushAndClear();
        today = kstDates.today();
        thisMonth = kstDates.firstDayOfThisMonth();
    }

    @Test
    void 데이터가_없으면_0건과_빈_목록이고_설정_행이_없어도_모집_중이_아닌_것으로_응답한다() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recruiting").value(false))
                .andExpect(jsonPath("$.counts.projects.current").value(0))
                .andExpect(jsonPath("$.counts.projects.past").value(0))
                .andExpect(jsonPath("$.counts.studies.current").value(0))
                .andExpect(jsonPath("$.counts.events.upcoming").value(0))
                .andExpect(jsonPath("$.counts.members.core").value(0))
                .andExpect(jsonPath("$.upcomingEvents").isEmpty())
                .andExpect(jsonPath("$.recentUpdates").isEmpty());
    }

    @Test
    void 모집_상태를_설정에서_읽는다() throws Exception {
        mockMvc.perform(put("/api/admin/settings").with(user("admin")).contentType(MediaType.APPLICATION_JSON).content("""
                        { "recruiting": true, "bannerText": "모집 중", "recruitPeriod": null,
                          "stats": [ { "label": "a", "value": 1 }, { "label": "b", "value": 2 },
                                     { "label": "c", "value": 3 }, { "label": "d", "value": 4 } ] }
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/dashboard").with(user("admin")))
                .andExpect(jsonPath("$.recruiting").value(true));
    }

    @Test
    void 프로젝트와_스터디는_이번_달_기준으로_현재와_과거를_센다() throws Exception {
        project("ongoing", null);
        project("ends-this-month", thisMonth);
        project("finished", thisMonth.minusMonths(1));
        study("study-ongoing", null);
        study("study-finished-1", thisMonth.minusMonths(1));
        study("study-finished-2", thisMonth.minusMonths(2));
        flushAndClear();

        mockMvc.perform(get("/api/admin/dashboard").with(user("admin")))
                .andExpect(jsonPath("$.counts.projects.current").value(2))
                .andExpect(jsonPath("$.counts.projects.past").value(1))
                .andExpect(jsonPath("$.counts.studies.current").value(1))
                .andExpect(jsonPath("$.counts.studies.past").value(2));
    }

    @Test
    void 일정은_종료일까지_다가오는_일정으로_세고_오늘이_경계다() throws Exception {
        event("today", today, null, 0);
        event("ongoing", today.minusDays(3), today, 0);
        event("yesterday", today.minusDays(1), null, 0);
        event("ended", today.minusDays(5), today.minusDays(1), 0);
        flushAndClear();

        mockMvc.perform(get("/api/admin/dashboard").with(user("admin")))
                .andExpect(jsonPath("$.counts.events.upcoming").value(2))
                .andExpect(jsonPath("$.counts.events.past").value(2));
    }

    @Test
    void 다가오는_일정은_최대_3건이고_진행_중을_포함해_가까운_순이며_같은_날은_표시_순서다() throws Exception {
        event("far", today.plusDays(9), null, 0);
        event("tomorrow-second", today.plusDays(1), null, 1);
        event("tomorrow-first", today.plusDays(1), null, 0);
        event("ongoing", today.minusDays(2), today.plusDays(1), 0);
        event("past", today.minusDays(2), null, 0);
        flushAndClear();

        mockMvc.perform(get("/api/admin/dashboard").with(user("admin")))
                .andExpect(jsonPath("$.counts.events.upcoming").value(4))
                .andExpect(jsonPath("$.upcomingEvents.length()").value(3))
                .andExpect(jsonPath("$.upcomingEvents[0].title").value("ongoing"))
                .andExpect(jsonPath("$.upcomingEvents[1].title").value("tomorrow-first"))
                .andExpect(jsonPath("$.upcomingEvents[2].title").value("tomorrow-second"))
                .andExpect(jsonPath("$.upcomingEvents[0].category").value("meeting"));
    }

    @Test
    void 멤버는_구분별로_센다() throws Exception {
        member("코어", MemberStatus.CORE, MemberRole.LEAD);
        member("시니어1", MemberStatus.SENIOR, null);
        member("시니어2", MemberStatus.SENIOR, null);
        flushAndClear();

        mockMvc.perform(get("/api/admin/dashboard").with(user("admin")))
                .andExpect(jsonPath("$.counts.members.core").value(1))
                .andExpect(jsonPath("$.counts.members.senior").value(2));
    }

    @Test
    void 최근_수정은_네_도메인을_합쳐_최신순_5건이고_같은_초의_순서도_유지하며_멤버_title은_이름이다() throws Exception {
        Long projectId = project("p", null).getId();
        Long studyId = study("s", null).getId();
        Long eventId = event("일정", today, null, 0).getId();
        Long memberId = member("홍길동", MemberStatus.CORE, MemberRole.LEAD).getId();
        Long oldProjectId = project("old", null).getId();
        Long oldestProjectId = project("oldest", null).getId();
        flushAndClear();
        Instant base = Instant.parse("2026-09-17T10:00:00Z");
        setUpdatedAt("member", memberId, base.plusMillis(900)); // 응답 형식으로 자르면 아래 일정과 같은 초다
        setUpdatedAt("event", eventId, base.plusMillis(100));
        setUpdatedAt("study", studyId, base.minusSeconds(10));
        setUpdatedAt("project", projectId, base.minusSeconds(20));
        setUpdatedAt("project", oldProjectId, base.minusSeconds(30));
        setUpdatedAt("project", oldestProjectId, base.minusSeconds(40));

        mockMvc.perform(get("/api/admin/dashboard").with(user("admin")))
                .andExpect(jsonPath("$.recentUpdates.length()").value(5))
                .andExpect(jsonPath("$.recentUpdates[0].resource").value("members"))
                .andExpect(jsonPath("$.recentUpdates[0].id").value(memberId))
                .andExpect(jsonPath("$.recentUpdates[0].title").value("홍길동"))
                .andExpect(jsonPath("$.recentUpdates[0].updatedAt").value("2026-09-17T19:00:00+09:00"))
                .andExpect(jsonPath("$.recentUpdates[1].resource").value("events"))
                .andExpect(jsonPath("$.recentUpdates[2].resource").value("studies"))
                .andExpect(jsonPath("$.recentUpdates[3].resource").value("projects"))
                .andExpect(jsonPath("$.recentUpdates[3].id").value(projectId))
                .andExpect(jsonPath("$.recentUpdates[4].id").value(oldProjectId));
    }

    @Test
    void 쿼리_수가_데이터_건수에_비례하지_않고_팀원을_불러오지_않는다() throws Exception {
        for (int i = 0; i < 20; i++) {
            Project project = project("p-" + i, null);
            project.addTeamMember("홍길동", "백엔드");
        }
        flushAndClear();
        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/admin/dashboard").with(user("admin"))).andExpect(status().isOk());

        // 건수 8 + 설정 1 + 다가오는 일정 1~2 + 최근 수정 4
        assertThat(statistics.getPrepareStatementCount()).isBetween(1L, 15L);
    }

    @Test
    void 인증_없이_요청하면_401이다() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private void setUpdatedAt(String table, Long id, Instant updatedAt) {
        jdbc.update("UPDATE " + table + " SET updated_at = ? WHERE id = ?", Timestamp.from(updatedAt), id);
    }

    private Project project(String slug, LocalDate periodEnd) {
        return projectRepository.save(Project.builder()
                .slug(slug).title(slug).summary("요약").category(ProjectCategory.TEAM_PROJECT)
                .periodStart(thisMonth.minusYears(1)).periodEnd(periodEnd)
                .description(List.of("소개")).techStack(List.of()).features(List.of()).outcomes(List.of())
                .build());
    }

    private Study study(String slug, LocalDate periodEnd) {
        return studyRepository.save(Study.builder()
                .slug(slug).title(slug).summary("요약").topic("Backend")
                .periodStart(thisMonth.minusYears(1)).periodEnd(periodEnd).leader("홍길동")
                .build());
    }

    private Event event(String title, LocalDate date, LocalDate endDate, int sortOrder) {
        return eventRepository.save(Event.builder()
                .title(title).category(EventCategory.MEETING).date(date).endDate(endDate).sortOrder(sortOrder)
                .build());
    }

    private Member member(String name, MemberStatus status, MemberRole role) {
        return memberRepository.save(Member.builder()
                .status(status).role(role).name(name).department("컴퓨터공학과")
                .history(List.of()).interests(List.of()).part(MemberPart.BACKEND).bio("소개")
                .consentConfirmedAt(Instant.parse("2026-03-01T00:00:00Z"))
                .build());
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
