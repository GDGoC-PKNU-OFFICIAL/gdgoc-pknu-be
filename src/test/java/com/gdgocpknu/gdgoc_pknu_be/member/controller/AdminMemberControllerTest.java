package com.gdgocpknu.gdgoc_pknu_be.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
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
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberPart;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRepository;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRole;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberRequest;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberRequest.LinksRequest;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 CRUD(A-42, A-43)를 실제 PostgreSQL 위에서 검증한다. 목록 필터 · 정렬은 AdminMemberListTest가 맡는다. */
@IntegrationTest
@Transactional
class AdminMemberControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    EntityManager em;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 등록하면_201과_Location_헤더와_저장된_값을_반환하고_공개_동의_일시를_기록한다() throws Exception {
        Instant before = Instant.now();

        MvcResult result = create(core("홍길동")).andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/admin/members/\\d+")))
                .andExpect(jsonPath("$.status").value("core"))
                .andExpect(jsonPath("$.role").value("part-lead"))
                .andExpect(jsonPath("$.part").value("backend"))
                .andExpect(jsonPath("$.history[0]").value("2026 파트 리드"))
                .andExpect(jsonPath("$.links.github").value("https://github.com/gildong"))
                .andExpect(jsonPath("$.consentConfirmedAt").value(endsWith("+09:00")))
                .andReturn();

        Long id = idOf(result);
        Instant consent = jdbc.queryForObject("SELECT consent_confirmed_at FROM member WHERE id = ?",
                Timestamp.class, id).toInstant();
        assertThat(consent).isBetween(before.minusSeconds(5), Instant.now().plusSeconds(5));
    }

    @Test
    void 인증_없이_등록하면_401이다() throws Exception {
        mockMvc.perform(post("/api/admin/members")
                        .contentType(MediaType.APPLICATION_JSON).content(json(core("홍길동"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 공개_동의를_확인하지_않으면_400이고_consentConfirmed에_에러가_달린다() throws Exception {
        create(modified(core("홍길동"), r -> new MemberRequest(r.status(), r.role(), r.name(), r.photoUrl(),
                r.department(), r.history(), r.interests(), r.part(), r.bio(), r.links(), false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='consentConfirmed')]").exists());
    }

    @Test
    void 필드가_빠진_요청은_400이다() throws Exception {
        String bodyWithoutBio = """
                { "status": "senior", "role": null, "name": "이시니", "department": "컴퓨터공학과",
                  "history": [], "interests": [], "part": "backend",
                  "links": {}, "consentConfirmed": true }
                """;

        mockMvc.perform(post("/api/admin/members").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(bodyWithoutBio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='bio')]").exists());
    }

    @Test
    void 코어멤버에_역할이_없으면_400이고_role에_에러가_달린다() throws Exception {
        create(modified(core("홍길동"), r -> new MemberRequest(r.status(), null, r.name(), r.photoUrl(),
                r.department(), r.history(), r.interests(), r.part(), r.bio(), r.links(), true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='role')]").exists());
    }

    @Test
    void 시니어에_역할이_있으면_400이다() throws Exception {
        create(modified(senior("이시니"), r -> new MemberRequest(r.status(), MemberRole.CORE, r.name(), r.photoUrl(),
                r.department(), r.history(), r.interests(), r.part(), r.bio(), r.links(), true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='role')]").exists());
    }

    @Test
    void 정의되지_않은_코드값은_500이_아니라_400이다() throws Exception {
        String badPart = """
                { "status": "senior", "role": null, "name": "이시니", "department": "컴퓨터공학과",
                  "history": [], "interests": [], "part": "devops", "bio": "소개",
                  "links": {}, "consentConfirmed": true }
                """;

        mockMvc.perform(post("/api/admin/members").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(badPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='part')]").exists());
    }

    @Test
    void 이력이_3개이거나_배열에_null_원소가_있으면_500이_아니라_400이다() throws Exception {
        String body = """
                { "status": "senior", "role": null, "name": "이시니", "department": "컴퓨터공학과",
                  "history": ["가", "나", "다"], "interests": ["a", null], "part": "backend", "bio": "소개",
                  "links": {}, "consentConfirmed": true }
                """;

        mockMvc.perform(post("/api/admin/members").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='history')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='interests[1]')]").exists());
    }

    @Test
    void 없는_id는_조회_수정_삭제_모두_404다() throws Exception {
        mockMvc.perform(get("/api/admin/members/999999").with(user("admin"))).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/admin/members/999999").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(core("홍길동"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/admin/members/999999").with(user("admin"))).andExpect(status().isNotFound());
    }

    @Test
    void 단건_조회는_수정_폼_초기값과_공개_동의_일시를_반환한다() throws Exception {
        Long id = idOf(create(core("홍길동")).andReturn());
        em.flush();
        em.clear();

        mockMvc.perform(get("/api/admin/members/{id}", id).with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.interests[1]").value("클라우드"))
                .andExpect(jsonPath("$.consentConfirmedAt").exists());
    }

    @Test
    void 수정하면_전체_필드가_교체되고_배열과_링크도_통째로_바뀐다() throws Exception {
        Long id = idOf(create(core("홍길동")).andReturn());

        MemberRequest update = new MemberRequest(
                MemberStatus.CORE, MemberRole.LEAD, "김리드", null, "소프트웨어학과",
                List.of(), List.of("AI"), MemberPart.AI_ML, "새 소개", new LinksRequest(null, "https://kim.dev"), true);

        mockMvc.perform(put("/api/admin/members/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김리드"))
                .andExpect(jsonPath("$.role").value("lead"))
                .andExpect(jsonPath("$.part").value("ai-ml"))
                .andExpect(jsonPath("$.photoUrl").doesNotExist())
                .andExpect(jsonPath("$.history").isEmpty())
                .andExpect(jsonPath("$.interests.length()").value(1))
                .andExpect(jsonPath("$.links.github").doesNotExist())
                .andExpect(jsonPath("$.links.homepage").value("https://kim.dev"));

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/admin/members/{id}", id).with(user("admin")))
                .andExpect(jsonPath("$.interests[0]").value("AI"))
                .andExpect(jsonPath("$.photoUrl").doesNotExist());
    }

    @Test
    void 수정해도_공개_동의_일시는_최초_값을_유지한다() throws Exception {
        Long id = idOf(create(core("홍길동")).andReturn());
        em.flush();
        jdbc.update("UPDATE member SET consent_confirmed_at = TIMESTAMPTZ '2025-01-01 00:00:00+09' WHERE id = ?", id);
        em.clear();

        mockMvc.perform(put("/api/admin/members/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(core("홍길동 수정"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consentConfirmedAt").value(startsWith("2025-01-01T00:00:00")));

        em.flush();
        assertThat(jdbc.queryForObject(
                "SELECT consent_confirmed_at = TIMESTAMPTZ '2025-01-01 00:00:00+09' FROM member WHERE id = ?",
                Boolean.class, id)).isTrue();
    }

    @Test
    void 시니어로_전환하면_역할이_null이어야_하고_값이_있으면_400이다() throws Exception {
        Long id = idOf(create(core("홍길동")).andReturn());

        MemberRequest withRole = new MemberRequest(
                MemberStatus.SENIOR, MemberRole.PART_LEAD, "홍길동", null, "컴퓨터공학과",
                List.of(), List.of(), MemberPart.BACKEND, "소개", new LinksRequest(null, null), true);
        mockMvc.perform(put("/api/admin/members/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(withRole)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='role')]").exists());

        MemberRequest withoutRole = new MemberRequest(
                MemberStatus.SENIOR, null, "홍길동", null, "컴퓨터공학과",
                List.of(), List.of(), MemberPart.BACKEND, "소개", new LinksRequest(null, null), true);
        mockMvc.perform(put("/api/admin/members/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(withoutRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("senior"))
                .andExpect(jsonPath("$.role").doesNotExist());
    }

    @Test
    void 시니어를_코어멤버로_바꿀_때_역할이_없으면_400이다() throws Exception {
        Long id = idOf(create(senior("이시니")).andReturn());

        MemberRequest toCore = new MemberRequest(
                MemberStatus.CORE, null, "이시니", null, "컴퓨터공학과",
                List.of(), List.of(), MemberPart.BACKEND, "소개", new LinksRequest(null, null), true);
        mockMvc.perform(put("/api/admin/members/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(toCore)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='role')]").exists());
    }

    @Test
    void 수정_요청에서_필드가_빠지면_400이고_기존_값은_바뀌지_않는다() throws Exception {
        Long id = idOf(create(core("홍길동")).andReturn());
        // PUT은 전체 필드 교체라 "안 보낸 필드는 그대로 둔다"로 해석하면 안 된다 (명세 1-4). history · interests · links 누락.
        String body = """
                { "status": "core", "role": "lead", "name": "바뀐 이름", "department": "컴퓨터공학과",
                  "part": "backend", "bio": "소개", "consentConfirmed": true }
                """;

        mockMvc.perform(put("/api/admin/members/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='history')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='interests')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='links')]").exists());

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/admin/members/{id}", id).with(user("admin")))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.history[0]").value("2026 파트 리드"));
    }

    @Test
    void 내용이_같은_수정도_updatedAt을_갱신한다() throws Exception {
        Long id = idOf(create(core("홍길동")).andReturn());
        em.flush();
        jdbc.update("UPDATE member SET updated_at = TIMESTAMPTZ '2020-01-01 00:00:00+09' WHERE id = ?", id);
        em.clear();

        mockMvc.perform(put("/api/admin/members/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(core("홍길동"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value(not(startsWith("2020-"))));
    }

    @Test
    void 요청에_서버_관리_값을_넣어도_무시한다() throws Exception {
        String body = """
                { "id": 999999, "status": "senior", "role": null, "name": "이시니", "department": "컴퓨터공학과",
                  "history": [], "interests": [], "part": "backend", "bio": "소개", "links": {},
                  "consentConfirmed": true, "consentConfirmedAt": "2000-01-01T00:00:00+09:00",
                  "createdAt": "2000-01-01T00:00:00+09:00", "updatedAt": "2000-01-01T00:00:00+09:00" }
                """;

        mockMvc.perform(post("/api/admin/members").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(999999)))
                .andExpect(jsonPath("$.consentConfirmedAt").value(not(startsWith("2000-"))))
                .andExpect(jsonPath("$.createdAt").value(not(startsWith("2000-"))));
    }

    @Test
    void 삭제하면_204고_다시_조회하면_404다() throws Exception {
        Long id = idOf(create(core("홍길동")).andReturn());
        em.flush();
        em.clear();

        mockMvc.perform(delete("/api/admin/members/{id}", id).with(user("admin")))
                .andExpect(status().isNoContent());
        em.flush();

        mockMvc.perform(get("/api/admin/members/{id}", id).with(user("admin"))).andExpect(status().isNotFound());
        assertThat(memberRepository.existsById(id)).isFalse();
    }

    @Test
    void 문자열은_앞뒤_공백을_제거한_뒤_검사하고_저장한다() throws Exception {
        String paddedName = "  " + "가".repeat(20) + "　 ";   // 공백 제외 20자 = 최대 길이
        MemberRequest request = new MemberRequest(
                MemberStatus.CORE, MemberRole.CORE, paddedName, null, " 컴퓨터공학과 ",
                List.of(" 이력 "), List.of(" Spring "), MemberPart.BACKEND, " 소개 ",
                new LinksRequest(" https://github.com/gildong ", null), true);

        create(request).andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("가".repeat(20)))
                .andExpect(jsonPath("$.department").value("컴퓨터공학과"))
                .andExpect(jsonPath("$.history[0]").value("이력"))
                .andExpect(jsonPath("$.interests[0]").value("Spring"))
                .andExpect(jsonPath("$.links.github").value("https://github.com/gildong"));
    }

    private ResultActions create(MemberRequest request) throws Exception {
        return mockMvc.perform(post("/api/admin/members").with(user("admin"))
                .contentType(MediaType.APPLICATION_JSON).content(json(request)));
    }

    private static Long idOf(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    private String json(MemberRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private static MemberRequest modified(MemberRequest request, UnaryOperator<MemberRequest> change) {
        return change.apply(request);
    }

    private static MemberRequest core(String name) {
        return new MemberRequest(
                MemberStatus.CORE, MemberRole.PART_LEAD, name, "https://example.r2.dev/members/a.webp", "컴퓨터공학과",
                List.of("2026 파트 리드"), List.of("백엔드", "클라우드"), MemberPart.BACKEND,
                "함께 만드는 개발자", new LinksRequest("https://github.com/gildong", null), true);
    }

    private static MemberRequest senior(String name) {
        return new MemberRequest(
                MemberStatus.SENIOR, null, name, null, "컴퓨터공학과",
                List.of(), List.of("클라우드"), MemberPart.BACKEND,
                "졸업해도 연결되는 개발자", new LinksRequest(null, null), true);
    }
}
