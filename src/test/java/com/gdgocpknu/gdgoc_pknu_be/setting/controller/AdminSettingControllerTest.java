package com.gdgocpknu.gdgoc_pknu_be.setting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSetting;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSettingRepository;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.StatValue;
import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingRequest;
import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingRequest.RecruitPeriodRequest;
import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingRequest.StatRequest;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 조회 · 수정(A-50)을 실제 PostgreSQL 위에서 검증한다. */
@IntegrationTest
@Transactional
class AdminSettingControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    SiteSettingRepository siteSettingRepository;
    @Autowired
    EntityManager em;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 인증_없이_조회_수정하면_401이다() throws Exception {
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(put("/api/admin/settings").contentType(MediaType.APPLICATION_JSON).content(json(request())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 설정_행이_없으면_조회는_404이고_수정하면_행이_만들어진다() throws Exception {
        mockMvc.perform(get("/api/admin/settings").with(user("admin"))).andExpect(status().isNotFound());

        update(request()).andExpect(status().isOk())
                .andExpect(jsonPath("$.bannerText").value("2026년 2학기 신입 부원 모집"))
                .andExpect(jsonPath("$.stats.length()").value(4));

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/admin/settings").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats[2].label").value("완료 프로젝트"));
        assertThat(count("site_setting")).isEqualTo(1);
        assertThat(count("site_stat")).isEqualTo(4);
    }

    @Test
    void 수정하면_전체_필드가_교체된다() throws Exception {
        seed();

        SettingRequest request = new SettingRequest(true, "새 배너",
                new RecruitPeriodRequest(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14)),
                List.of(new StatRequest("기수", 7, null, "기"), new StatRequest("멤버", 300, "+", "명"),
                        new StatRequest("프로젝트", 21, null, null), new StatRequest("행사", 50, null, null)));

        update(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.recruiting").value(true))
                .andExpect(jsonPath("$.bannerText").value("새 배너"))
                .andExpect(jsonPath("$.recruitPeriod.start").value("2026-09-01"))
                .andExpect(jsonPath("$.recruitPeriod.end").value("2026-09-14"))
                .andExpect(jsonPath("$.stats[*].label", contains("기수", "멤버", "프로젝트", "행사")))
                .andExpect(jsonPath("$.stats[1].value").value(300))
                .andExpect(jsonPath("$.stats[1].unit").value("+"))
                .andExpect(jsonPath("$.stats[2].unit").doesNotExist());

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/admin/settings").with(user("admin")))
                .andExpect(jsonPath("$.stats[3].label").value("행사"));
    }

    @Test
    void 통계를_여러_번_수정해도_행이_늘지_않고_제자리에서_바뀌며_UNIQUE_충돌이_없다() throws Exception {
        seed();
        em.flush();
        List<Long> before = jdbc.queryForList("SELECT id FROM site_stat ORDER BY sort_order", Long.class);

        update(request()).andExpect(status().isOk());
        em.flush();
        update(request("둘째 수정")).andExpect(status().isOk());
        em.flush();
        em.clear();

        assertThat(jdbc.queryForList("SELECT id FROM site_stat ORDER BY sort_order", Long.class)).isEqualTo(before);
        assertThat(jdbc.queryForList("SELECT sort_order FROM site_stat ORDER BY sort_order", Integer.class))
                .containsExactly(1, 2, 3, 4);
        assertThat(count("site_setting")).isEqualTo(1);
    }

    @Test
    void 모집_기간을_null로_보내면_기간이_삭제된다() throws Exception {
        seed();
        update(request()).andExpect(jsonPath("$.recruitPeriod.start").exists());

        SettingRequest withoutPeriod = new SettingRequest(false, "모집 마감", null, request().stats());
        update(withoutPeriod).andExpect(status().isOk())
                .andExpect(jsonPath("$.recruitPeriod").doesNotExist());

        em.flush();
        assertThat(jdbc.queryForObject("SELECT recruit_start IS NULL AND recruit_end IS NULL FROM site_setting",
                Boolean.class)).isTrue();
    }

    @Test
    void 모집_기간이_하나만_있거나_순서가_거꾸로면_400이다() throws Exception {
        seed();

        mockMvc.perform(put("/api/admin/settings").with(user("admin")).contentType(MediaType.APPLICATION_JSON)
                        .content(raw("\"recruitPeriod\": {\"start\": \"2026-09-01\", \"end\": null}", stats(4))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='recruitPeriod.end')]").exists());

        mockMvc.perform(put("/api/admin/settings").with(user("admin")).contentType(MediaType.APPLICATION_JSON)
                        .content(raw("\"recruitPeriod\": {\"start\": \"2026-09-30\", \"end\": \"2026-09-01\"}", stats(4))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='recruitPeriod.end')]").exists());
    }

    @Test
    void 없는_날짜나_형식이_틀린_날짜는_500이_아니라_400이다() throws Exception {
        mockMvc.perform(put("/api/admin/settings").with(user("admin")).contentType(MediaType.APPLICATION_JSON)
                        .content(raw("\"recruitPeriod\": {\"start\": \"2026-02-30\", \"end\": \"2026-03-01\"}", stats(4))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 통계가_4개가_아니거나_없으면_400이다() throws Exception {
        seed();

        mockMvc.perform(put("/api/admin/settings").with(user("admin")).contentType(MediaType.APPLICATION_JSON)
                        .content(raw("\"recruitPeriod\": null", stats(3))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='stats')]").exists());

        mockMvc.perform(put("/api/admin/settings").with(user("admin")).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"recruiting\": true, \"bannerText\": \"배너\", \"recruitPeriod\": null }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='stats')]").exists());
    }

    @Test
    void 통계_value가_음수이거나_정수가_아니면_400이다() throws Exception {
        // 1.5 · 1.0 · "7"은 Jackson 기본 동작이면 1 · 1 · 7로 조용히 바뀌어 저장되는 값이다.
        for (String value : List.of("-1", "1.5", "1.0", "\"abc\"", "\"7\"", "true", "null", "3000000000")) {
            String stats = "[" + stat("기수", value) + "," + stat("멤버", "0") + "," + stat("프로젝트", "0") + ","
                    + stat("행사", "0") + "]";
            mockMvc.perform(put("/api/admin/settings").with(user("admin")).contentType(MediaType.APPLICATION_JSON)
                            .content(raw("\"recruitPeriod\": null", stats)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Test
    void 통계_value는_0과_int_최댓값까지_정수로_받는다() throws Exception {
        seed();
        String stats = "[" + stat("기수", "0") + "," + stat("멤버", "2147483647") + "," + stat("프로젝트", "7") + ","
                + stat("행사", "0") + "]";

        mockMvc.perform(put("/api/admin/settings").with(user("admin")).contentType(MediaType.APPLICATION_JSON)
                        .content(raw("\"recruitPeriod\": null", stats)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats[0].value").value(0))
                .andExpect(jsonPath("$.stats[1].value").value(2147483647))
                .andExpect(jsonPath("$.stats[2].value").value(7));
    }

    @Test
    void 통계_label이_비어_있거나_배너_문구가_길면_400이다() throws Exception {
        String stats = "[" + stat("", "1") + "," + stat("멤버", "0") + "," + stat("프로젝트", "0") + "," + stat("행사", "0") + "]";
        String body = """
                { "recruiting": true, "bannerText": "%s", "recruitPeriod": null, "stats": %s }
                """.formatted("가".repeat(61), stats);

        mockMvc.perform(put("/api/admin/settings").with(user("admin")).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='bannerText')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='stats[0].label')]").exists());
    }

    @Test
    void 필드가_빠진_수정은_400이고_기존_값은_바뀌지_않는다() throws Exception {
        seed();
        em.flush();
        em.clear();
        // PUT은 전체 필드 교체라 "안 보낸 필드는 그대로 둔다"로 해석하면 안 된다 (명세 1-4). recruiting 누락.
        String body = "{ \"bannerText\": \"바뀐 배너\", \"recruitPeriod\": null, \"stats\": " + stats(4) + " }";

        mockMvc.perform(put("/api/admin/settings").with(user("admin")).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='recruiting')]").exists());

        mockMvc.perform(get("/api/admin/settings").with(user("admin")))
                .andExpect(jsonPath("$.bannerText").value("기존 배너"));
    }

    @Test
    void 통계만_바꿔도_updatedAt이_갱신된다() throws Exception {
        seed();
        em.flush();
        jdbc.update("UPDATE site_setting SET updated_at = TIMESTAMPTZ '2020-01-01 00:00:00+09' WHERE id = 1");
        em.clear();

        // 배너 · 모집 상태 · 기간은 그대로 두고 통계 값만 바꾼다.
        SettingRequest statsOnly = new SettingRequest(false, "기존 배너", null,
                List.of(new StatRequest("운영 기수", 9, null, null), new StatRequest("누적 멤버", 0, null, null),
                        new StatRequest("완료 프로젝트", 0, null, null), new StatRequest("세션·이벤트", 0, null, null)));

        update(statsOnly).andExpect(status().isOk())
                .andExpect(jsonPath("$.stats[0].value").value(9))
                .andExpect(jsonPath("$.updatedAt").value(not(startsWith("2020-"))));
    }

    @Test
    void 내용이_같은_수정도_updatedAt을_갱신한다() throws Exception {
        seed();
        em.flush();
        jdbc.update("UPDATE site_setting SET updated_at = TIMESTAMPTZ '2020-01-01 00:00:00+09' WHERE id = 1");
        em.clear();

        SettingRequest same = new SettingRequest(false, "기존 배너", null, seedStatRequests());

        update(same).andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value(not(startsWith("2020-"))));
    }

    @Test
    void 문자열은_앞뒤_공백을_제거한_뒤_검사하고_빈_unit_suffix는_생략한다() throws Exception {
        seed();
        SettingRequest padded = new SettingRequest(true, "  배너  ", null,
                List.of(new StatRequest("  운영 기수  ", 1, "  ", ""), new StatRequest("멤버", 0, " + ", null),
                        new StatRequest("프로젝트", 0, null, null), new StatRequest("행사", 0, null, null)));

        update(padded).andExpect(status().isOk())
                .andExpect(jsonPath("$.bannerText").value("배너"))
                .andExpect(jsonPath("$.stats[0].label").value("운영 기수"))
                .andExpect(jsonPath("$.stats[0].unit").doesNotExist())
                .andExpect(jsonPath("$.stats[0].suffix").doesNotExist())
                .andExpect(jsonPath("$.stats[1].unit").value("+"));
    }

    // ───────────────────────────── 도우미 ─────────────────────────────

    private void seed() {
        siteSettingRepository.save(SiteSetting.create(false, "기존 배너", null, null, List.of(
                new StatValue("운영 기수", 1, null, null), new StatValue("누적 멤버", 0, null, null),
                new StatValue("완료 프로젝트", 0, null, null), new StatValue("세션·이벤트", 0, null, null))));
    }

    private static List<StatRequest> seedStatRequests() {
        return List.of(new StatRequest("운영 기수", 1, null, null), new StatRequest("누적 멤버", 0, null, null),
                new StatRequest("완료 프로젝트", 0, null, null), new StatRequest("세션·이벤트", 0, null, null));
    }

    private ResultActions update(SettingRequest request) throws Exception {
        return mockMvc.perform(put("/api/admin/settings").with(user("admin"))
                .contentType(MediaType.APPLICATION_JSON).content(json(request)));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private String json(SettingRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private static SettingRequest request() {
        return request("2026년 2학기 신입 부원 모집");
    }

    private static SettingRequest request(String banner) {
        return new SettingRequest(true, banner,
                new RecruitPeriodRequest(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14)),
                List.of(new StatRequest("운영 기수", 3, null, "기"), new StatRequest("누적 멤버", 120, null, "명"),
                        new StatRequest("완료 프로젝트", 15, null, null), new StatRequest("세션·이벤트", 40, null, null)));
    }

    private static String stat(String label, String value) {
        return "{ \"label\": \"" + label + "\", \"value\": " + value + " }";
    }

    private static String stats(int count) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            builder.append(i == 0 ? "" : ",").append(stat("통계" + i, String.valueOf(i)));
        }
        return builder.append("]").toString();
    }

    /** 요청 모양을 직접 만들어 record로는 보낼 수 없는 값(소수 · 문자열 · 누락)을 시험한다. */
    private static String raw(String recruitPeriodFragment, String statsJson) {
        return "{ \"recruiting\": true, \"bannerText\": \"배너\", " + recruitPeriodFragment
                + ", \"stats\": " + statsJson + " }";
    }
}
