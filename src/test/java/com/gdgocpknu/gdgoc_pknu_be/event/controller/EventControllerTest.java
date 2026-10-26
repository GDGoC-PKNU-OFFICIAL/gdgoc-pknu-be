package com.gdgocpknu.gdgoc_pknu_be.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 실제 PostgreSQL을 통과하는 공개 조회 검증. 저장 후 영속성 컨텍스트를 비워 DB 왕복을 거치게 한다. */
@IntegrationTest
@Transactional
class EventControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    EventRepository eventRepository;
    @Autowired
    EntityManager em;

    @Test
    void 인증_없이_전체_목록을_날짜_오름차순_같은_날은_sortOrder_순으로_반환한다() throws Exception {
        LocalDate day = LocalDate.of(2026, 9, 20);
        save(event("later-day", day.plusDays(1), 0));
        save(event("second", day, 1));
        save(event("past", day.minusYears(1), 0));
        save(event("first", day, 0));
        flushAndClear();

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", contains("past", "first", "second", "later-day")));
    }

    @Test
    void 명세_3_3의_필드_모양으로_직렬화된다() throws Exception {
        save(Event.builder()
                .title("GDGoC 세미나").category(EventCategory.PRESENTATION)
                .date(LocalDate.of(2026, 9, 20)).endDate(LocalDate.of(2026, 9, 21))
                .time("19:00–21:00").place("공학관 101호").summary("한 줄 요약").sortOrder(0)
                .build());
        flushAndClear();

        mockMvc.perform(get("/api/events"))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].category").value("presentation"))
                .andExpect(jsonPath("$[0].date").value("2026-09-20"))
                .andExpect(jsonPath("$[0].endDate").value("2026-09-21"))
                .andExpect(jsonPath("$[0].time").value("19:00–21:00"))
                .andExpect(jsonPath("$[0].place").value("공학관 101호"))
                .andExpect(jsonPath("$[0].summary").value("한 줄 요약"))
                .andExpect(jsonPath("$[0].sortOrder").value(0))
                .andExpect(jsonPath("$[0].createdAt").value(endsWith("+09:00")))
                .andExpect(jsonPath("$[0].updatedAt").value(endsWith("+09:00")));
    }

    @Test
    void 값이_없는_선택_필드는_생략한다() throws Exception {
        save(event("minimal", LocalDate.of(2026, 9, 20), 0));
        flushAndClear();

        mockMvc.perform(get("/api/events"))
                .andExpect(jsonPath("$[0].endDate").doesNotExist())
                .andExpect(jsonPath("$[0].time").doesNotExist())
                .andExpect(jsonPath("$[0].place").doesNotExist())
                .andExpect(jsonPath("$[0].summary").doesNotExist());
    }

    @Test
    void 일정이_없으면_빈_배열이다() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void 목록_조회는_한_번의_쿼리로_끝난다() throws Exception {
        for (int i = 0; i < 8; i++) {
            save(event("event-" + i, LocalDate.of(2026, 1, 1).plusDays(i), 0));
        }
        flushAndClear();
        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/events")).andExpect(jsonPath("$.length()").value(8));

        // 자식 컬렉션이 없으므로 목록 1회뿐이다. 0이 아닌 정확한 값을 기대해 통계가 꺼진 경우도 잡는다.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
    }

    private void save(Event event) {
        eventRepository.save(event);
    }

    private static Event event(String title, LocalDate date, int sortOrder) {
        return Event.builder().title(title).category(EventCategory.MEETING).date(date).sortOrder(sortOrder).build();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
