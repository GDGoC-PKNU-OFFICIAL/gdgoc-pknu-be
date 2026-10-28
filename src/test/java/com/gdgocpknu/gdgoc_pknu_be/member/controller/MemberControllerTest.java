package com.gdgocpknu.gdgoc_pknu_be.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
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
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 실제 PostgreSQL을 통과하는 공개 조회 검증. 저장 후 영속성 컨텍스트를 비워 jsonb · 컨버터 왕복을 확인한다. */
@IntegrationTest
@Transactional
class MemberControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    EntityManager em;

    @Test
    void 인증_없이_전체_목록을_구분_역할_이름_순으로_반환한다() throws Exception {
        save(senior("이시니"));
        save(core("김코어", MemberRole.CORE));
        save(core("박파트", MemberRole.PART_LEAD));
        save(core("최리드", MemberRole.LEAD));
        save(core("강코어", MemberRole.CORE));
        flushAndClear();

        mockMvc.perform(get("/api/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("최리드", "박파트", "강코어", "김코어", "이시니")));
    }

    @Test
    void 명세_3_4의_필드_모양으로_직렬화되고_공개_동의_일시는_없다() throws Exception {
        save(Member.builder()
                .status(MemberStatus.CORE).role(MemberRole.PART_LEAD).name("홍길동")
                .photoUrl("https://example.r2.dev/members/a.webp").department("컴퓨터공학과")
                .history(List.of("2026 파트 리드", "솔루션 챌린지 수상")).interests(List.of("LLM", "백엔드", "클라우드"))
                .part(MemberPart.AI_ML).bio("함께 만드는 개발자")
                .githubUrl("https://github.com/gildong").homepageUrl("https://gildong.dev")
                .consentConfirmedAt(Instant.parse("2026-03-01T00:00:00Z")));
        flushAndClear();

        mockMvc.perform(get("/api/members"))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].status").value("core"))
                .andExpect(jsonPath("$[0].role").value("part-lead"))
                .andExpect(jsonPath("$[0].part").value("ai-ml"))
                .andExpect(jsonPath("$[0].photoUrl").value("https://example.r2.dev/members/a.webp"))
                .andExpect(jsonPath("$[0].history[1]").value("솔루션 챌린지 수상"))
                .andExpect(jsonPath("$[0].interests[2]").value("클라우드"))
                .andExpect(jsonPath("$[0].links.github").value("https://github.com/gildong"))
                .andExpect(jsonPath("$[0].links.homepage").value("https://gildong.dev"))
                .andExpect(jsonPath("$[0].createdAt").value(endsWith("+09:00")))
                .andExpect(jsonPath("$[0].updatedAt").value(endsWith("+09:00")))
                .andExpect(jsonPath("$[0].consentConfirmedAt").doesNotExist());
    }

    @Test
    void 시니어는_역할과_값이_없는_선택_필드를_생략하고_배열은_빈_배열로_내려준다() throws Exception {
        save(senior("이시니"));
        flushAndClear();

        mockMvc.perform(get("/api/members"))
                .andExpect(jsonPath("$[0].status").value("senior"))
                .andExpect(jsonPath("$[0].role").doesNotExist())
                .andExpect(jsonPath("$[0].photoUrl").doesNotExist())
                .andExpect(jsonPath("$[0].links.github").doesNotExist())
                .andExpect(jsonPath("$[0].links.homepage").doesNotExist())
                .andExpect(jsonPath("$[0].history").isEmpty())
                .andExpect(jsonPath("$[0].interests").isEmpty());
    }

    @Test
    void 멤버가_없으면_빈_배열이다() throws Exception {
        mockMvc.perform(get("/api/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void 목록_조회는_한_번의_쿼리로_끝난다() throws Exception {
        for (int i = 0; i < 8; i++) {
            save(core("멤버" + i, MemberRole.CORE));
        }
        flushAndClear();
        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/members")).andExpect(jsonPath("$.length()").value(8));

        // 자식 컬렉션이 없고 jsonb는 같은 행에서 읽으므로 8건이어도 쿼리는 1회다. 하한(1)은 통계가 꺼진 경우를 잡는다.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
    }

    private void save(Member.MemberBuilder builder) {
        memberRepository.save(builder.build());
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
