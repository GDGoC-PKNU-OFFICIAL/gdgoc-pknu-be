package com.gdgocpknu.gdgoc_pknu_be.study.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gdgocpknu.gdgoc_pknu_be.study.domain.Study;
import com.gdgocpknu.gdgoc_pknu_be.study.domain.StudyRepository;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Clock;
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
class StudyControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    StudyRepository studyRepository;
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

        mockMvc.perform(get("/api/studies"))
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

        mockMvc.perform(get("/api/studies"))
                .andExpect(jsonPath("$[?(@.slug=='ongoing')].status").value("current"))
                .andExpect(jsonPath("$[?(@.slug=='ends-this-month')].status").value("current"))
                .andExpect(jsonPath("$[?(@.slug=='finished')].status").value("past"));
    }

    @Test
    void 명세_3_2의_필드_모양으로_직렬화되고_참여자_산출물_순서가_유지된다() throws Exception {
        Study study = study("spring-study", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 6, 1));
        study.addParticipant("김부경");
        study.addParticipant("이서울");
        study.addParticipant("박해운");
        study.addOutput("회고록", "https://velog.io/@gdgoc/retrospective");
        study.addOutput("자료", "https://github.com/gdgoc-pknu/spring-study");
        studyRepository.save(study);
        flushAndClear();

        mockMvc.perform(get("/api/studies"))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].topic").value("Backend"))
                .andExpect(jsonPath("$[0].leader").value("홍길동"))
                .andExpect(jsonPath("$[0].period.start").value("2025.03"))
                .andExpect(jsonPath("$[0].period.end").value("2025.06"))
                .andExpect(jsonPath("$[0].participants[0]").value("김부경"))
                .andExpect(jsonPath("$[0].participants[1]").value("이서울"))
                .andExpect(jsonPath("$[0].participants[2]").value("박해운"))
                .andExpect(jsonPath("$[0].outputs[0].label").value("회고록"))
                .andExpect(jsonPath("$[0].outputs[1].url").value("https://github.com/gdgoc-pknu/spring-study"))
                .andExpect(jsonPath("$[0].createdAt").value(endsWith("+09:00")))
                .andExpect(jsonPath("$[0].updatedAt").value(endsWith("+09:00")));
    }

    @Test
    void 값이_없는_선택_필드는_생략하고_배열은_빈_배열로_내려준다() throws Exception {
        studyRepository.save(Study.builder()
                .slug("minimal").title("제목").summary("요약").topic("AI")
                .periodStart(LocalDate.of(2026, 3, 1)).leader("홍길동")
                .build());
        flushAndClear();

        mockMvc.perform(get("/api/studies"))
                .andExpect(jsonPath("$[0].period.end").doesNotExist())
                .andExpect(jsonPath("$[0].format").doesNotExist())
                .andExpect(jsonPath("$[0].participants").isEmpty())
                .andExpect(jsonPath("$[0].outputs").isEmpty());
    }

    @Test
    void 스터디가_없으면_빈_배열이다() throws Exception {
        mockMvc.perform(get("/api/studies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void 목록_조회_쿼리_수는_스터디_수에_비례하지_않는다() throws Exception {
        LocalDate start = LocalDate.of(2025, 3, 1);
        for (int i = 0; i < 8; i++) {
            Study study = study("study-" + i, start.plusMonths(i), null);
            study.addParticipant("참여자A" + i);
            study.addParticipant("참여자B" + i);
            study.addOutput("자료" + i, "https://example.com/" + i);
            studyRepository.save(study);
        }
        flushAndClear();
        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/studies")).andExpect(jsonPath("$.length()").value(8));

        // 스터디 목록 1회 + 참여자 batch fetch 1회 + 산출물 batch fetch 1회. N+1이면 8건에 17회 이상이 된다.
        // 하한(1)은 통계가 꺼져 있어 0으로 "통과"하는 경우를 잡는다.
        assertThat(statistics.getPrepareStatementCount()).isBetween(1L, 3L);
    }

    private void save(String slug, LocalDate start, LocalDate end) {
        studyRepository.save(study(slug, start, end));
    }

    private static Study study(String slug, LocalDate start, LocalDate end) {
        return Study.builder()
                .slug(slug).title("스프링 스터디").summary("요약").topic("Backend")
                .periodStart(start).periodEnd(end).leader("홍길동")
                .build();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
