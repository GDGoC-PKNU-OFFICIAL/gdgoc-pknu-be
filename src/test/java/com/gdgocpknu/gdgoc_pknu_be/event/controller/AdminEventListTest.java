package com.gdgocpknu.gdgoc_pknu_be.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gdgocpknu.gdgoc_pknu_be.event.domain.Event;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventCategory;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventRepository;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import jakarta.persistence.EntityManager;
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
 * 관리자 목록(A-31)의 필터 · 정렬 · 잘못된 파라미터 · 쿼리 수를 실제 PostgreSQL 위에서 검증한다 (API 명세서 1-6, 2-6).
 * CRUD · 순서 변경은 AdminEventControllerTest가 맡는다.
 */
@IntegrationTest
@Transactional
class AdminEventListTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 20);

    @Autowired
    MockMvc mockMvc;
    @Autowired
    EventRepository eventRepository;
    @Autowired
    EntityManager em;
    @Autowired
    JdbcTemplate jdbc;

    // ───────────────────────────── 필터 ─────────────────────────────

    @Test
    void q는_제목을_대소문자_구분_없이_부분_일치로_찾는다() throws Exception {
        save(event("GDGoC Seminar"));
        save(event("정기 모임"));
        flushAndClear();

        mockMvc.perform(list().param("q", "  seminar "))
                .andExpect(jsonPath("$.items[*].title", contains("GDGoC Seminar")))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void q의_퍼센트와_밑줄은_와일드카드가_아니라_글자로_찾는다() throws Exception {
        save(event("참석률 100% 달성"));
        save(event("참석률 1000 달성"));
        flushAndClear();

        mockMvc.perform(list().param("q", "100%"))
                .andExpect(jsonPath("$.items[*].title", contains("참석률 100% 달성")));
        mockMvc.perform(list().param("q", "_"))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void 빈_q는_검색하지_않는다() throws Exception {
        save(event("a"));
        save(event("b"));
        flushAndClear();

        mockMvc.perform(list().param("q", "   "))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void category는_코드값으로_거른다() throws Exception {
        save(event("세미나").category(EventCategory.SEMINAR));
        save(event("축제").category(EventCategory.FESTIVAL));
        flushAndClear();

        mockMvc.perform(list().param("category", "festival"))
                .andExpect(jsonPath("$.items[*].title", contains("축제")))
                .andExpect(jsonPath("$.items[0].category").value("festival"));
    }

    @Test
    void year는_시작일의_연도로_거르고_경계_날짜를_포함한다() throws Exception {
        save(event("jan-1-2025").date(LocalDate.of(2025, 1, 1)));
        save(event("dec-31-2025").date(LocalDate.of(2025, 12, 31)));
        save(event("dec-31-2024").date(LocalDate.of(2024, 12, 31)).endDate(LocalDate.of(2025, 1, 2)));
        save(event("jan-1-2026").date(LocalDate.of(2026, 1, 1)));
        flushAndClear();

        mockMvc.perform(list().param("year", "2025"))
                .andExpect(jsonPath("$.items[*].title", contains("dec-31-2025", "jan-1-2025")));
    }

    @Test
    void 여러_조건은_AND로_결합한다() throws Exception {
        save(event("스프링 세미나").category(EventCategory.SEMINAR).date(LocalDate.of(2025, 5, 1)));
        save(event("스프링 세미나 작년").category(EventCategory.SEMINAR).date(LocalDate.of(2024, 5, 1)));
        save(event("스프링 축제").category(EventCategory.FESTIVAL).date(LocalDate.of(2025, 5, 1)));
        flushAndClear();

        mockMvc.perform(list().param("q", "스프링").param("year", "2025").param("category", "seminar"))
                .andExpect(jsonPath("$.items[*].title", contains("스프링 세미나")));
    }

    // ───────────────────────────── 정렬 ─────────────────────────────

    @Test
    void 기본_정렬은_날짜_최신순이고_같은_날짜는_sortOrder_오름차순이다() throws Exception {
        save(event("old").date(DAY.minusDays(10)));
        save(event("same-1").date(DAY).sortOrder(1));
        save(event("new").date(DAY.plusDays(10)));
        save(event("same-0").date(DAY).sortOrder(0));
        flushAndClear();

        mockMvc.perform(list())
                .andExpect(jsonPath("$.items[*].title", contains("new", "same-0", "same-1", "old")));
    }

    @Test
    void sort로_제목_오름차순_정렬한다() throws Exception {
        save(event("다"));
        save(event("가"));
        save(event("나"));
        flushAndClear();

        mockMvc.perform(list().param("sort", "title,asc"))
                .andExpect(jsonPath("$.items[*].title", contains("가", "나", "다")));
    }

    @Test
    void sort로_최근_수정순_정렬한다() throws Exception {
        Long first = save(event("first"));
        Long second = save(event("second"));
        flushAndClear();
        jdbc.update("UPDATE event SET updated_at = TIMESTAMPTZ '2026-01-01 00:00:00+09' WHERE id = ?", first);
        jdbc.update("UPDATE event SET updated_at = TIMESTAMPTZ '2025-01-01 00:00:00+09' WHERE id = ?", second);

        mockMvc.perform(list().param("sort", "updatedAt,desc"))
                .andExpect(jsonPath("$.items[*].title", contains("first", "second")));
    }

    @Test
    void 날짜와_순서가_같으면_id_순으로_고정되어_페이지가_겹치지_않는다() throws Exception {
        save(event("p1"));
        save(event("p2"));
        save(event("p3"));
        flushAndClear();

        mockMvc.perform(list().param("size", "2").param("page", "0"))
                .andExpect(jsonPath("$.items[*].title", contains("p1", "p2")));
        mockMvc.perform(list().param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.items[*].title", contains("p3")));
    }

    // ───────────────────────── 잘못된 파라미터 ─────────────────────────

    @Test
    void 허용하지_않는_정렬_필드는_400이다() throws Exception {
        mockMvc.perform(list().param("sort", "sortOrder,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"));
    }

    @Test
    void 정의되지_않은_category_코드값은_400이다() throws Exception {
        mockMvc.perform(list().param("category", "workshop"))
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

    // ───────────────────────────── 쿼리 수 ─────────────────────────────

    @Test
    void 목록_조회는_목록과_건수_두_번의_쿼리로_끝난다() throws Exception {
        for (int i = 0; i < 25; i++) {
            save(event("일정 " + i).date(DAY.plusDays(i)));
        }
        flushAndClear();
        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(list().param("size", "20").param("q", "일정").param("category", "meeting"))
                .andExpect(jsonPath("$.items.length()").value(20))
                .andExpect(jsonPath("$.totalItems").value(25));

        // 목록 1회 + 전체 건수(count) 1회. 자식 컬렉션이 없어 N+1 여지는 없지만, 연관이 추가되면 여기서 드러난다.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2L);
    }

    @Test
    void 인증_없이_목록을_조회하면_401이다() throws Exception {
        mockMvc.perform(get("/api/admin/events"))
                .andExpect(status().isUnauthorized());
    }

    // ───────────────────────────── 도우미 ─────────────────────────────

    private static MockHttpServletRequestBuilder list() {
        return get("/api/admin/events").with(user("admin"));
    }

    private Long save(Event.EventBuilder builder) {
        return eventRepository.save(builder.build()).getId();
    }

    private static Event.EventBuilder event(String title) {
        return Event.builder().title(title).category(EventCategory.MEETING).date(DAY).sortOrder(0);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
