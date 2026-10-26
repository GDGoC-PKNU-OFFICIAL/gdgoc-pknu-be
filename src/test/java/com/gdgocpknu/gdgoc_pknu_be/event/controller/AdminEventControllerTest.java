package com.gdgocpknu.gdgoc_pknu_be.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventCategory;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventRepository;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventOrderRequest;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventRequest;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 CRUD(A-32, A-33)와 같은 날짜 순서 변경(A-31 `PUT /order`)을 실제 PostgreSQL 위에서 검증한다.
 * 목록 필터 · 정렬은 AdminEventListTest가 맡는다.
 */
@IntegrationTest
@Transactional
class AdminEventControllerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 20);

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    EventRepository eventRepository;
    @Autowired
    EntityManager em;
    @Autowired
    JdbcTemplate jdbc;

    // ───────────────────────────── 등록 ─────────────────────────────

    @Test
    void 등록하면_201과_Location_헤더와_저장된_값을_반환한다() throws Exception {
        mockMvc.perform(post("/api/admin/events").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request("세미나", DAY))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/admin/events/\\d+")))
                .andExpect(jsonPath("$.title").value("세미나"))
                .andExpect(jsonPath("$.category").value("seminar"))
                .andExpect(jsonPath("$.date").value("2026-09-20"))
                .andExpect(jsonPath("$.endDate").value("2026-09-21"))
                .andExpect(jsonPath("$.time").value("19:00–21:00"))
                .andExpect(jsonPath("$.sortOrder").value(0));
    }

    @Test
    void 같은_날짜에_등록하면_마지막_순서_다음_번호를_받는다() throws Exception {
        create(request("첫째", DAY)).andExpect(jsonPath("$.sortOrder").value(0));
        create(request("둘째", DAY)).andExpect(jsonPath("$.sortOrder").value(1));
        create(request("다른 날", DAY.plusDays(1))).andExpect(jsonPath("$.sortOrder").value(0));
        create(request("셋째", DAY)).andExpect(jsonPath("$.sortOrder").value(2));
    }

    @Test
    void 중간_일정을_삭제해도_다음_등록은_마지막_순서_다음이다() throws Exception {
        createAndGetId(request("첫째", DAY));
        Long middle = createAndGetId(request("둘째", DAY));
        createAndGetId(request("셋째", DAY));
        mockMvc.perform(delete("/api/admin/events/{id}", middle).with(user("admin"))).andExpect(status().isNoContent());

        create(request("넷째", DAY)).andExpect(jsonPath("$.sortOrder").value(3));
    }

    @Test
    void 선택_필드를_빈_문자열이나_null로_보내면_응답에서_생략된다() throws Exception {
        String body = """
                { "title": "정기 모임", "category": "meeting", "date": "2026-09-20",
                  "endDate": null, "time": "", "place": "   ", "summary": null }
                """;

        mockMvc.perform(post("/api/admin/events").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.endDate").doesNotExist())
                .andExpect(jsonPath("$.time").doesNotExist())
                .andExpect(jsonPath("$.place").doesNotExist())
                .andExpect(jsonPath("$.summary").doesNotExist());
    }

    @Test
    void 문자열은_앞뒤_공백을_제거한_뒤_검사하고_저장한다() throws Exception {
        String body = """
                { "title": "  %s　 ", "category": "meeting", "date": " 2026-09-20 ",
                  "endDate": null, "time": " 19:00 ", "place": " 공학관 ", "summary": null }
                """.formatted("가".repeat(40));

        mockMvc.perform(post("/api/admin/events").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("가".repeat(40)))
                .andExpect(jsonPath("$.date").value("2026-09-20"))
                .andExpect(jsonPath("$.time").value("19:00"))
                .andExpect(jsonPath("$.place").value("공학관"));
    }

    // ───────────────────────────── 검증 ─────────────────────────────

    @Test
    void 필드가_빠진_요청은_400이다() throws Exception {
        String bodyWithoutTitleAndDate = """
                { "category": "meeting", "endDate": null, "time": null, "place": null, "summary": null }
                """;

        mockMvc.perform(post("/api/admin/events").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(bodyWithoutTitleAndDate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='title')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='date')]").exists());
    }

    @Test
    void 종료일이_시작일보다_빠르면_400이고_endDate에_에러가_달린다() throws Exception {
        mockMvc.perform(post("/api/admin/events").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new EventRequest("세미나", EventCategory.SEMINAR, DAY, DAY.minusDays(1),
                                null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("endDate"));
    }

    @Test
    void 형식이_틀리거나_없는_날짜는_500이_아니라_400이다() throws Exception {
        for (String date : List.of("2026-02-30", "2026/09/20", "2026-9-20")) {
            mockMvc.perform(post("/api/admin/events").with(user("admin"))
                            .contentType(MediaType.APPLICATION_JSON).content(rawBody("meeting", date)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("date"));
        }
    }

    @Test
    void 정의되지_않은_category_코드값은_400이다() throws Exception {
        mockMvc.perform(post("/api/admin/events").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(rawBody("workshop", "2026-09-20")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("category"));
    }

    @Test
    void 인증_없이_등록하면_401이다() throws Exception {
        mockMvc.perform(post("/api/admin/events")
                        .contentType(MediaType.APPLICATION_JSON).content(json(request("세미나", DAY))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ───────────────────────────── 조회 · 수정 · 삭제 ─────────────────────────────

    @Test
    void 단건_조회는_수정_폼_초기값을_반환한다() throws Exception {
        Long id = createAndGetId(request("세미나", DAY));
        em.flush();
        em.clear();

        mockMvc.perform(get("/api/admin/events/{id}", id).with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.place").value("공학관 101호"));
    }

    @Test
    void 없는_id는_조회_수정_삭제_모두_404다() throws Exception {
        mockMvc.perform(get("/api/admin/events/999999").with(user("admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mockMvc.perform(put("/api/admin/events/999999").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request("세미나", DAY))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/admin/events/999999").with(user("admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void 수정하면_전체_필드가_교체되고_날짜가_같으면_순서를_유지한다() throws Exception {
        createAndGetId(request("첫째", DAY));
        Long id = createAndGetId(request("둘째", DAY));

        EventRequest update = new EventRequest("바뀐 제목", EventCategory.FESTIVAL, DAY, null, null, "대운동장", null);
        mockMvc.perform(put("/api/admin/events/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("바뀐 제목"))
                .andExpect(jsonPath("$.category").value("festival"))
                .andExpect(jsonPath("$.endDate").doesNotExist())
                .andExpect(jsonPath("$.time").doesNotExist())
                .andExpect(jsonPath("$.place").value("대운동장"))
                .andExpect(jsonPath("$.summary").doesNotExist())
                .andExpect(jsonPath("$.sortOrder").value(1));
    }

    @Test
    void 날짜를_바꾸면_새_날짜의_마지막_순서로_이동한다() throws Exception {
        LocalDate other = DAY.plusDays(3);
        createAndGetId(request("기존 1", other));
        createAndGetId(request("기존 2", other));
        Long id = createAndGetId(request("옮길 일정", DAY));

        mockMvc.perform(put("/api/admin/events/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request("옮길 일정", other))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(other.toString()))
                .andExpect(jsonPath("$.sortOrder").value(2));

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/events"))
                .andExpect(jsonPath("$[*].title", contains("기존 1", "기존 2", "옮길 일정")));
    }

    @Test
    void 수정_응답의_updatedAt은_수정_후_값이다() throws Exception {
        Long id = createAndGetId(request("세미나", DAY));
        em.flush();
        jdbc.update("UPDATE event SET updated_at = TIMESTAMPTZ '2020-01-01 00:00:00+09' WHERE id = ?", id);
        em.clear();

        mockMvc.perform(put("/api/admin/events/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request("바뀐 세미나", DAY))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value(not(startsWith("2020-"))));
    }

    @Test
    void 삭제하면_204고_같은_날짜의_남은_순서는_그대로다() throws Exception {
        Long first = createAndGetId(request("첫째", DAY));
        Long second = createAndGetId(request("둘째", DAY));
        Long third = createAndGetId(request("셋째", DAY));
        em.flush();
        em.clear();

        mockMvc.perform(delete("/api/admin/events/{id}", second).with(user("admin")))
                .andExpect(status().isNoContent());
        em.flush();

        assertThat(eventRepository.existsById(second)).isFalse();
        assertThat(sortOrderOf(first)).isZero();
        assertThat(sortOrderOf(third)).isEqualTo(2);
    }

    // ───────────────────────────── 순서 변경 ─────────────────────────────

    @Test
    void 순서를_바꾸면_204고_보낸_순서대로_0부터_다시_매긴다() throws Exception {
        Long a = createAndGetId(request("A", DAY));
        Long b = createAndGetId(request("B", DAY));
        Long c = createAndGetId(request("C", DAY));
        Long otherDay = createAndGetId(request("다른 날", DAY.plusDays(1)));

        reorder(new EventOrderRequest(DAY, List.of(c, a, b))).andExpect(status().isNoContent());
        em.flush();
        em.clear();

        assertThat(List.of(sortOrderOf(c), sortOrderOf(a), sortOrderOf(b))).containsExactly(0, 1, 2);
        assertThat(sortOrderOf(otherDay)).isZero();
        mockMvc.perform(get("/api/events"))
                .andExpect(jsonPath("$[*].title", contains("C", "A", "B", "다른 날")));
    }

    @Test
    void 삭제로_번호가_비어_있어도_순서_변경은_0부터_연속으로_매긴다() throws Exception {
        Long a = createAndGetId(request("A", DAY));
        Long b = createAndGetId(request("B", DAY));
        Long c = createAndGetId(request("C", DAY));
        mockMvc.perform(delete("/api/admin/events/{id}", b).with(user("admin"))).andExpect(status().isNoContent());

        reorder(new EventOrderRequest(DAY, List.of(c, a))).andExpect(status().isNoContent());
        em.flush();
        em.clear();

        assertThat(List.of(sortOrderOf(c), sortOrderOf(a))).containsExactly(0, 1);
    }

    @Test
    void 순서_변경_id_목록이_그_날짜의_일정과_정확히_일치하지_않으면_400이고_순서는_그대로다() throws Exception {
        Long a = createAndGetId(request("A", DAY));
        Long b = createAndGetId(request("B", DAY));
        Long otherDay = createAndGetId(request("다른 날", DAY.plusDays(1)));

        List<List<Long>> mismatches = List.of(
                List.of(b),                  // 누락
                List.of(b, a, otherDay),     // 다른 날짜 id
                List.of(b, a, 999999L),      // 없는 id
                List.of(b, otherDay));       // 개수는 같지만 다른 날짜 id
        for (List<Long> ids : mismatches) {
            reorder(new EventOrderRequest(DAY, ids))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("orderedIds"));
        }

        em.flush();
        em.clear();
        assertThat(List.of(sortOrderOf(a), sortOrderOf(b))).containsExactly(0, 1);
    }

    @Test
    void 순서_변경_id_목록에_중복이_있으면_400이다() throws Exception {
        Long a = createAndGetId(request("A", DAY));
        Long b = createAndGetId(request("B", DAY));

        reorder(new EventOrderRequest(DAY, List.of(a, b, a)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("orderedIds"));
    }

    @Test
    void 일정이_없는_날짜에_빈_목록을_보내면_204다() throws Exception {
        reorder(new EventOrderRequest(DAY, List.of())).andExpect(status().isNoContent());
    }

    @Test
    void 인증_없이_순서를_바꾸면_401이다() throws Exception {
        mockMvc.perform(put("/api/admin/events/order")
                        .contentType(MediaType.APPLICATION_JSON).content(json(new EventOrderRequest(DAY, List.of()))))
                .andExpect(status().isUnauthorized());
    }

    // ───────────────────────────── 도우미 ─────────────────────────────

    private ResultActions create(EventRequest request) throws Exception {
        return mockMvc.perform(post("/api/admin/events").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated());
    }

    private Long createAndGetId(EventRequest request) throws Exception {
        MvcResult result = create(request).andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    private ResultActions reorder(EventOrderRequest request) throws Exception {
        return mockMvc.perform(put("/api/admin/events/order").with(user("admin"))
                .contentType(MediaType.APPLICATION_JSON).content(json(request)));
    }

    private int sortOrderOf(Long id) {
        return jdbc.queryForObject("SELECT sort_order FROM event WHERE id = ?", Integer.class, id);
    }

    private String json(Object request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    /** 날짜 · 코드값을 문자열 그대로 보내기 위해 JSON을 직접 만든다. */
    private static String rawBody(String category, String date) {
        return """
                { "title": "세미나", "category": "%s", "date": "%s",
                  "endDate": null, "time": null, "place": null, "summary": null }
                """.formatted(category, date);
    }

    private static EventRequest request(String title, LocalDate date) {
        return new EventRequest(title, EventCategory.SEMINAR, date, date.plusDays(1),
                "19:00–21:00", "공학관 101호", "한 줄 요약");
    }
}
