package com.gdgocpknu.gdgoc_pknu_be.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gdgocpknu.gdgoc_pknu_be.member.domain.Member;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberPart;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRepository;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRole;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 목록(A-41)의 기본 정렬 · 필터 · 정렬 · 잘못된 파라미터 · 쿼리 수를 실제 PostgreSQL 위에서 검증한다
 * (API 명세서 1-6, 2-7). CRUD 자체는 AdminMemberControllerTest가 맡는다.
 */
@IntegrationTest
@Transactional
class AdminMemberListTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    EntityManager em;
    @Autowired
    JdbcTemplate jdbc;

    // ───────────────────────────── 기본 정렬 ─────────────────────────────

    @Test
    void 기본_정렬은_구분_역할_이름_순이다() throws Exception {
        save(senior("나시니"));
        save(core("다코어", MemberRole.CORE));
        save(core("라파트", MemberRole.PART_LEAD));
        save(core("가리드", MemberRole.LEAD));
        save(core("가코어", MemberRole.CORE));
        save(senior("가시니"));
        flushAndClear();

        mockMvc.perform(list())
                .andExpect(jsonPath("$.items[*].name", contains("가리드", "라파트", "가코어", "다코어", "가시니", "나시니")));
    }

    @Test
    void 기본_정렬은_페이지를_나눠도_유지되고_전체_건수가_맞다() throws Exception {
        save(senior("마시니"));
        save(core("다코어", MemberRole.CORE));
        save(core("라파트", MemberRole.PART_LEAD));
        save(core("가리드", MemberRole.LEAD));
        save(core("가코어", MemberRole.CORE));
        flushAndClear();

        mockMvc.perform(list().param("size", "2").param("page", "0"))
                .andExpect(jsonPath("$.items[*].name", contains("가리드", "라파트")))
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
        mockMvc.perform(list().param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.items[*].name", contains("가코어", "다코어")));
        mockMvc.perform(list().param("size", "2").param("page", "2"))
                .andExpect(jsonPath("$.items[*].name", contains("마시니")));
    }

    @Test
    void 필터와_기본_정렬을_함께_써도_정렬이_유지된다() throws Exception {
        save(core("다코어", MemberRole.CORE));
        save(core("가코어", MemberRole.CORE));
        save(core("나리드", MemberRole.LEAD));
        save(senior("가시니"));
        flushAndClear();

        mockMvc.perform(list().param("status", "core"))
                .andExpect(jsonPath("$.items[*].name", contains("나리드", "가코어", "다코어")))
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    // ───────────────────────────── 필터 ─────────────────────────────

    @Test
    void q는_이름을_대소문자_구분_없이_부분_일치로_찾는다() throws Exception {
        save(core("Kim Gildong", MemberRole.CORE));
        save(core("박부경", MemberRole.CORE));
        flushAndClear();

        mockMvc.perform(list().param("q", "gILD"))
                .andExpect(jsonPath("$.items[*].name", contains("Kim Gildong")))
                .andExpect(jsonPath("$.totalItems").value(1));
        mockMvc.perform(list().param("q", "  부경 "))
                .andExpect(jsonPath("$.items[*].name", contains("박부경")));
    }

    @Test
    void q의_퍼센트와_밑줄은_와일드카드가_아니라_글자로_찾는다() throws Exception {
        save(core("100% 열정", MemberRole.CORE));
        save(core("1000 열정", MemberRole.CORE));
        flushAndClear();

        mockMvc.perform(list().param("q", "100%"))
                .andExpect(jsonPath("$.items[*].name", contains("100% 열정")));
        mockMvc.perform(list().param("q", "_"))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void 빈_q는_검색하지_않는다() throws Exception {
        save(core("가", MemberRole.CORE));
        save(senior("나"));
        flushAndClear();

        mockMvc.perform(list().param("q", "   "))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void status는_코드값으로_거른다() throws Exception {
        save(core("가코어", MemberRole.CORE));
        save(senior("나시니"));
        flushAndClear();

        mockMvc.perform(list().param("status", "senior"))
                .andExpect(jsonPath("$.items[*].name", contains("나시니")));
    }

    @Test
    void role은_코드값으로_거른다() throws Exception {
        save(core("가리드", MemberRole.LEAD));
        save(core("나파트", MemberRole.PART_LEAD));
        save(core("다코어", MemberRole.CORE));
        flushAndClear();

        mockMvc.perform(list().param("role", "part-lead"))
                .andExpect(jsonPath("$.items[*].name", contains("나파트")));
    }

    @Test
    void 여러_조건은_AND로_결합한다() throws Exception {
        save(core("김리드", MemberRole.LEAD));
        save(core("김코어", MemberRole.CORE));
        save(core("박리드", MemberRole.LEAD));
        save(senior("김시니"));
        flushAndClear();

        mockMvc.perform(list().param("q", "김").param("status", "core").param("role", "lead"))
                .andExpect(jsonPath("$.items[*].name", contains("김리드")));
    }

    // ───────────────────────────── 정렬 ─────────────────────────────

    @Test
    void sort로_이름_오름차순_내림차순_정렬한다() throws Exception {
        save(senior("다"));
        save(core("가", MemberRole.CORE));
        save(core("나", MemberRole.LEAD));
        flushAndClear();

        mockMvc.perform(list().param("sort", "name,asc"))
                .andExpect(jsonPath("$.items[*].name", contains("가", "나", "다")));
        mockMvc.perform(list().param("sort", "name,desc"))
                .andExpect(jsonPath("$.items[*].name", contains("다", "나", "가")));
    }

    @Test
    void sort로_최근_수정순_정렬한다() throws Exception {
        Long first = save(core("첫째", MemberRole.CORE));
        Long second = save(core("둘째", MemberRole.CORE));
        flushAndClear();
        jdbc.update("UPDATE member SET updated_at = TIMESTAMPTZ '2026-01-01 00:00:00+09' WHERE id = ?", first);
        jdbc.update("UPDATE member SET updated_at = TIMESTAMPTZ '2025-01-01 00:00:00+09' WHERE id = ?", second);

        mockMvc.perform(list().param("sort", "updatedAt,desc"))
                .andExpect(jsonPath("$.items[*].name", contains("첫째", "둘째")));
    }

    @Test
    void 이름이_같으면_id_역순으로_고정되어_페이지가_겹치지_않는다() throws Exception {
        save(core("동명", MemberRole.CORE));
        save(core("동명", MemberRole.CORE));
        save(core("동명", MemberRole.CORE));
        flushAndClear();

        String page0 = mockMvc.perform(list().param("sort", "name,asc").param("size", "2").param("page", "0"))
                .andReturn().getResponse().getContentAsString();
        String page1 = mockMvc.perform(list().param("sort", "name,asc").param("size", "2").param("page", "1"))
                .andReturn().getResponse().getContentAsString();

        List<Integer> ids = new ArrayList<>();
        for (String body : List.of(page0, page1)) {
            Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(body);
            while (matcher.find()) {
                ids.add(Integer.valueOf(matcher.group(1)));
            }
        }
        assertThat(ids).hasSize(3).doesNotHaveDuplicates();
    }

    // ───────────────────────── 잘못된 파라미터 ─────────────────────────

    @Test
    void 허용하지_않는_정렬_필드는_400이다() throws Exception {
        mockMvc.perform(list().param("sort", "part,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"));
    }

    @Test
    void 정의되지_않은_status_role_코드값은_400이다() throws Exception {
        mockMvc.perform(list().param("status", "alumni"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
        mockMvc.perform(list().param("role", "captain"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("role"));
    }

    @Test
    void 인증_없이_목록을_조회하면_401이다() throws Exception {
        mockMvc.perform(get("/api/admin/members"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ───────────────────────────── 쿼리 수 ─────────────────────────────

    @Test
    void 목록_조회는_목록과_건수_두_번의_쿼리로_끝난다() throws Exception {
        for (int i = 0; i < 25; i++) {
            save(core("멤버" + i, MemberRole.CORE));
        }
        flushAndClear();
        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(list().param("size", "20").param("q", "멤버"))
                .andExpect(jsonPath("$.items.length()").value(20))
                .andExpect(jsonPath("$.totalItems").value(25));

        // 목록 1회 + 전체 건수(count) 1회. 자식 컬렉션이 없어 N+1이 없다. 하한(1)은 통계가 꺼진 경우를 잡는다.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2L);
    }

    // ───────────────────────────── 도우미 ─────────────────────────────

    private static MockHttpServletRequestBuilder list() {
        return get("/api/admin/members").with(user("admin"));
    }

    private Long save(Member.MemberBuilder builder) {
        return memberRepository.save(builder.build()).getId();
    }

    private static Member.MemberBuilder core(String name, MemberRole role) {
        return Member.builder()
                .status(MemberStatus.CORE).role(role).name(name).department("컴퓨터공학과")
                .history(List.of()).interests(List.of()).part(MemberPart.BACKEND).bio("소개")
                .consentConfirmedAt(Instant.parse("2026-03-01T00:00:00Z"));
    }

    private static Member.MemberBuilder senior(String name) {
        return Member.builder()
                .status(MemberStatus.SENIOR).name(name).department("컴퓨터공학과")
                .history(List.of()).interests(List.of()).part(MemberPart.BACKEND).bio("소개")
                .consentConfirmedAt(Instant.parse("2026-03-01T00:00:00Z"));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
