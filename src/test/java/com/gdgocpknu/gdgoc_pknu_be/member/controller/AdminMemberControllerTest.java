package com.gdgocpknu.gdgoc_pknu_be.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
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
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRepository;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberRequest;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberRequest.LinksRequest;
import com.gdgocpknu.gdgoc_pknu_be.support.FakeS3Client;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
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

    private static final String PHOTO_KEY = "members/8f1c0000-0000-4000-8000-000000000001.webp";

    @Autowired
    FakeS3Client s3;

    @BeforeEach
    void registerPhoto() {
        s3.reset();
        s3.put(PHOTO_KEY, 1024, "image/webp");
    }

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
                .andExpect(jsonPath("$.sortOrder").value(1))
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.part").value("백엔드"))
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
        create(modified(core("홍길동"), r -> new MemberRequest(r.status(), r.name(), r.photoUrl(),
                r.department(), r.history(), r.interests(), r.part(), r.bio(), r.links(), false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='consentConfirmed')]").exists());
    }

    @Test
    void 필드가_빠진_요청은_400이다() throws Exception {
        String bodyWithoutBio = """
                { "status": "senior", "name": "이시니", "department": "컴퓨터공학과",
                  "history": [], "interests": [], "part": "backend",
                  "links": {}, "consentConfirmed": true }
                """;

        mockMvc.perform(post("/api/admin/members").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(bodyWithoutBio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='bio')]").exists());
    }

    @Test
    void 정의되지_않은_구분_코드값은_500이_아니라_400이다() throws Exception {
        String badStatus = """
                { "status": "alumni", "name": "이시니", "department": "컴퓨터공학과",
                  "history": [], "interests": [], "part": "백엔드", "bio": "소개",
                  "links": {}, "consentConfirmed": true }
                """;

        mockMvc.perform(post("/api/admin/members").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(badStatus))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='status')]").exists());
    }

    @Test
    void 분야는_직접_적은_글자를_그대로_저장하고_비우거나_20자를_넘으면_400이다() throws Exception {
        create(modified(core("홍길동"), r -> withPart(r, "DevOps · 인프라"))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.part").value("DevOps · 인프라"));
        create(modified(core("홍길동"), r -> withPart(r, "  "))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='part')]").exists());
        create(modified(core("홍길동"), r -> withPart(r, "가".repeat(21)))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='part')]").exists());
    }

    @Test
    void 이력이_3개이거나_배열에_null_원소가_있으면_500이_아니라_400이다() throws Exception {
        String body = """
                { "status": "senior", "name": "이시니", "department": "컴퓨터공학과",
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
                MemberStatus.CORE, "김리드", null, "소프트웨어학과",
                List.of(), List.of("AI"), "AI·ML", "새 소개", new LinksRequest(null, "kim@example.com"), true);

        mockMvc.perform(put("/api/admin/members/{id}", id).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김리드"))
                .andExpect(jsonPath("$.part").value("AI·ML"))
                .andExpect(jsonPath("$.photoUrl").doesNotExist())
                .andExpect(jsonPath("$.history").isEmpty())
                .andExpect(jsonPath("$.interests.length()").value(1))
                .andExpect(jsonPath("$.links.github").doesNotExist())
                .andExpect(jsonPath("$.links.email").value("kim@example.com"));

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
    void 등록하면_같은_구분의_맨_뒤_순서를_받는다() throws Exception {
        create(core("가")).andExpect(jsonPath("$.sortOrder").value(1));
        create(core("나")).andExpect(jsonPath("$.sortOrder").value(2));
        // 구분마다 따로 센다
        create(senior("다")).andExpect(jsonPath("$.sortOrder").value(1));
    }

    @Test
    void 구분을_바꾸면_바뀐_구분의_맨_뒤로_가고_같은_구분_수정은_순서를_그대로_둔다() throws Exception {
        Long core = idOf(create(core("홍길동")).andReturn());
        create(senior("이시니"));
        create(senior("박시니"));

        mockMvc.perform(put("/api/admin/members/{id}", core).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(senior("홍길동"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("senior"))
                .andExpect(jsonPath("$.sortOrder").value(3));

        mockMvc.perform(put("/api/admin/members/{id}", core).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(senior("홍길동 수정"))))
                .andExpect(jsonPath("$.sortOrder").value(3));
    }

    @Test
    void 순서를_바꾸면_204이고_목록과_공개_화면이_그_순서가_된다() throws Exception {
        Long a = idOf(create(core("가")).andReturn());
        Long b = idOf(create(core("나")).andReturn());
        Long c = idOf(create(core("다")).andReturn());
        create(senior("시니"));

        reorder("core", List.of(c, a, b)).andExpect(status().isNoContent());
        em.flush();
        em.clear();

        mockMvc.perform(get("/api/admin/members").with(user("admin")).param("status", "core"))
                .andExpect(jsonPath("$.items[*].name", contains("다", "가", "나")))
                .andExpect(jsonPath("$.items[*].sortOrder", contains(1, 2, 3)));
        mockMvc.perform(get("/api/members"))
                .andExpect(jsonPath("$[*].name", contains("다", "가", "나", "시니")));
    }

    @Test
    void 순서_목록이_그_구분의_멤버와_다르면_400이고_순서는_그대로다() throws Exception {
        Long a = idOf(create(core("가")).andReturn());
        Long b = idOf(create(core("나")).andReturn());
        Long senior = idOf(create(senior("시니")).andReturn());

        // 빠진 멤버
        reorder("core", List.of(b)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("ids"));
        // 다른 구분의 멤버
        reorder("core", List.of(b, a, senior)).andExpect(status().isBadRequest());
        // 같은 id 두 번
        reorder("core", List.of(b, b)).andExpect(status().isBadRequest());

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/admin/members").with(user("admin")).param("status", "core"))
                .andExpect(jsonPath("$.items[*].name", contains("가", "나")));
    }

    @Test
    void 인증_없이_순서를_바꾸면_401이다() throws Exception {
        mockMvc.perform(put("/api/admin/members/order")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"core\",\"ids\":[1]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 수정_요청에서_필드가_빠지면_400이고_기존_값은_바뀌지_않는다() throws Exception {
        Long id = idOf(create(core("홍길동")).andReturn());
        // PUT은 전체 필드 교체라 "안 보낸 필드는 그대로 둔다"로 해석하면 안 된다 (명세 1-4). history · interests · links 누락.
        String body = """
                { "status": "core", "name": "바뀐 이름", "department": "컴퓨터공학과",
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
                { "id": 999999, "status": "senior", "name": "이시니", "department": "컴퓨터공학과",
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
                MemberStatus.CORE, paddedName, null, " 컴퓨터공학과 ",
                List.of(" 이력 "), List.of(" Spring "), "백엔드", " 소개 ",
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

    private ResultActions reorder(String status, List<Long> ids) throws Exception {
        return mockMvc.perform(put("/api/admin/members/order").with(user("admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("status", status, "ids", ids))));
    }

    private static Long idOf(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    private String json(MemberRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private static MemberRequest withPart(MemberRequest r, String part) {
        return new MemberRequest(r.status(), r.name(), r.photoUrl(), r.department(), r.history(), r.interests(),
                part, r.bio(), r.links(), r.consentConfirmed());
    }

    private static MemberRequest modified(MemberRequest request, UnaryOperator<MemberRequest> change) {
        return change.apply(request);
    }

    private static MemberRequest core(String name) {
        return new MemberRequest(
                MemberStatus.CORE, name, "https://test-bucket.r2.dev/" + PHOTO_KEY, "컴퓨터공학과",
                List.of("2026 파트 리드"), List.of("백엔드", "클라우드"), "백엔드",
                "함께 만드는 개발자", new LinksRequest("https://github.com/gildong", null), true);
    }

    private static MemberRequest senior(String name) {
        return new MemberRequest(
                MemberStatus.SENIOR, name, null, "컴퓨터공학과",
                List.of(), List.of("클라우드"), "백엔드",
                "졸업해도 연결되는 개발자", new LinksRequest(null, null), true);
    }
}
