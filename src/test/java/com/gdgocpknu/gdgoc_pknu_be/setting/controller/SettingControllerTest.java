package com.gdgocpknu.gdgoc_pknu_be.setting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSetting;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSettingRepository;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.StatValue;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 실제 PostgreSQL을 통과하는 공개 조회 검증. 저장 후 영속성 컨텍스트를 비워 DB 왕복을 거치게 한다. */
@IntegrationTest
@Transactional
class SettingControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    SiteSettingRepository siteSettingRepository;
    @Autowired
    EntityManager em;

    @Test
    void 인증_없이_설정을_명세_3_5의_모양으로_반환한다() throws Exception {
        siteSettingRepository.save(SiteSetting.create(true, "2026년 2학기 신입 부원 모집",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14),
                List.of(new StatValue("운영 기수", 3, null, "기"),
                        new StatValue("누적 멤버", 120, "+", "명"),
                        new StatValue("완료 프로젝트", 15, null, null),
                        new StatValue("세션·이벤트", 40, null, null))));
        flushAndClear();

        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recruiting").value(true))
                .andExpect(jsonPath("$.bannerText").value("2026년 2학기 신입 부원 모집"))
                .andExpect(jsonPath("$.recruitPeriod.start").value("2026-09-01"))
                .andExpect(jsonPath("$.recruitPeriod.end").value("2026-09-14"))
                .andExpect(jsonPath("$.stats.length()").value(4))
                .andExpect(jsonPath("$.stats[*].label", contains("운영 기수", "누적 멤버", "완료 프로젝트", "세션·이벤트")))
                .andExpect(jsonPath("$.stats[0].value").value(3))
                .andExpect(jsonPath("$.stats[0].suffix").value("기"))
                .andExpect(jsonPath("$.stats[1].unit").value("+"))
                .andExpect(jsonPath("$.updatedAt").value(endsWith("+09:00")));
    }

    @Test
    void 모집_기간과_값이_없는_unit_suffix는_생략한다() throws Exception {
        siteSettingRepository.save(SiteSetting.create(false, "모집 마감", null, null, stats()));
        flushAndClear();

        mockMvc.perform(get("/api/settings"))
                .andExpect(jsonPath("$.recruiting").value(false))
                .andExpect(jsonPath("$.recruitPeriod").doesNotExist())
                .andExpect(jsonPath("$.stats[0].unit").doesNotExist())
                .andExpect(jsonPath("$.stats[0].suffix").doesNotExist());
    }

    @Test
    void 통계는_입력한_배열_순서대로_내려준다() throws Exception {
        siteSettingRepository.save(SiteSetting.create(false, "모집 마감", null, null, List.of(
                new StatValue("넷째", 4, null, null), new StatValue("첫째", 1, null, null),
                new StatValue("둘째", 2, null, null), new StatValue("셋째", 3, null, null))));
        flushAndClear();

        // 입력 배열 순서가 곧 표시 순서다(라벨 이름과 무관). 읽을 때는 sortOrder로 정렬되어 같은 순서가 나온다.
        mockMvc.perform(get("/api/settings"))
                .andExpect(jsonPath("$.stats[*].label", contains("넷째", "첫째", "둘째", "셋째")));
    }

    @Test
    void 설정_행이_없으면_404다() throws Exception {
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void 조회_쿼리는_설정과_통계_두_번으로_끝난다() throws Exception {
        siteSettingRepository.save(SiteSetting.create(false, "모집 마감", null, null, stats()));
        flushAndClear();
        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/settings")).andExpect(jsonPath("$.stats.length()").value(4));

        // 설정 1회 + 통계 1회. 하한(1)은 통계가 꺼져 0으로 "통과"하는 경우를 잡는다.
        assertThat(statistics.getPrepareStatementCount()).isBetween(1L, 2L);
    }

    private static List<StatValue> stats() {
        return List.of(new StatValue("운영 기수", 1, null, null), new StatValue("누적 멤버", 0, null, null),
                new StatValue("완료 프로젝트", 0, null, null), new StatValue("세션·이벤트", 0, null, null));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
