package com.gdgocpknu.gdgoc_pknu_be.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectCategory;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectRequest;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectRequest.LinksRequest;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectRequest.PeriodRequest;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectRequest.TeamMemberRequest;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 요청 본문이 메모리를 얼마나 쓸 수 있는지의 상한({@link JacksonConfig})을 확인한다. 이 상한이 없으면 인증 없이 부르는
 * 로그인에 큰 본문을 동시에 보내 힙을 바닥낼 수 있다(검증 `@Size`는 문자열을 다 읽은 뒤에야 돈다).
 * 반대로 상한이 너무 낮으면 관리자 폼이 깨지므로, 검증이 허용하는 가장 큰 요청이 통과하는지도 함께 본다.
 */
@IntegrationTest
class JacksonLimitsTest {

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    Validator validator;
    @Autowired
    MockMvc mockMvc;

    @Test
    void 문자열은_상한까지만_읽고_하나만_넘어도_거절한다() throws Exception {
        String atLimit = json("a".repeat(JacksonConfig.MAX_STRING_LENGTH));
        assertThat(objectMapper.readValue(atLimit, Map.class)).containsKey("v");

        String overLimit = json("a".repeat(JacksonConfig.MAX_STRING_LENGTH + 1));
        assertThatThrownBy(() -> objectMapper.readValue(overLimit, Map.class))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void 본문_전체_크기가_상한을_넘으면_거절한다() {
        String chunk = "\"" + "a".repeat(1_000) + "\"";
        String body = "[" + String.join(",", java.util.Collections.nCopies(300, chunk)) + "]";
        assertThat(body.length()).isGreaterThan(JacksonConfig.MAX_DOCUMENT_LENGTH);

        // 컬렉션으로 읽으면 JsonMappingException으로 감싸져 나오므로 종류가 아니라 메시지로 구분한다.
        assertThatThrownBy(() -> objectMapper.readValue(body, List.class))
                .hasMessageContaining("Document length")
                .hasMessageContaining("exceeds the maximum allowed");
    }

    @Test
    void 중첩이_너무_깊으면_거절한다() {
        int depth = JacksonConfig.MAX_NESTING_DEPTH + 1;
        String body = "[".repeat(depth) + "]".repeat(depth);

        assertThatThrownBy(() -> objectMapper.readValue(body, Object.class))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void 검증이_허용하는_가장_큰_프로젝트_요청은_상한_안에서_읽히고_검증도_통과한다() throws Exception {
        String body = objectMapper.writeValueAsString(largestValidProject());

        ProjectRequest read = objectMapper.readValue(body, ProjectRequest.class);

        assertThat(validator.validate(read)).isEmpty();
        assertThat(body.length()).as("정상 요청의 가장 큰 크기는 상한보다 한참 작아야 한다")
                .isLessThan(JacksonConfig.MAX_DOCUMENT_LENGTH / 2);
    }

    @Test
    void 로그인에_큰_본문을_보내면_500이_아니라_400으로_거절한다() throws Exception {
        for (int size : new int[] {JacksonConfig.MAX_STRING_LENGTH + 1, 1_000_000}) {
            String body = "{\"loginId\":\"admin\",\"password\":\"" + "a".repeat(size) + "\"}";

            mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    private static String json(String value) {
        return "{\"v\":\"" + value + "\"}";
    }

    /** 검증 어노테이션이 허용하는 최댓값으로 모든 필드를 채운 요청. 상한을 정할 때의 기준이다. */
    private static ProjectRequest largestValidProject() {
        return new ProjectRequest(
                "가".repeat(40),
                "a".repeat(60),
                "가".repeat(80),
                ProjectCategory.OFFICIAL,
                new PeriodRequest("2026.03", "2026.04"),
                "https://" + "a".repeat(492),
                java.util.Collections.nCopies(10, "가".repeat(1_000)),
                java.util.Collections.nCopies(20, new TeamMemberRequest("가".repeat(20), "가".repeat(20))),
                IntStream.range(0, 10).mapToObj(i -> String.valueOf((char) ('a' + i)).repeat(20)).toList(),
                java.util.Collections.nCopies(10, "가".repeat(80)),
                java.util.Collections.nCopies(10, "가".repeat(80)),
                new LinksRequest("https://" + "a".repeat(492), "https://" + "b".repeat(492)), null);
    }
}
