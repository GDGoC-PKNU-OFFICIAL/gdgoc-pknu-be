package com.gdgocpknu.gdgoc_pknu_be.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.SQLException;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Spring 컨텍스트 · DB 없이 에러 응답 모양(명세 1-7)만 검증한다. */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void 검증_실패는_400과_필드_경로_그대로의_fieldErrors를_반환한다() throws Exception {
        String body = """
                { "title": "", "history": ["ok", "너무긴이력"], "team": [ { "name": "" } ] }
                """;

        mockMvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해 주세요."))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='title')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='history[1]')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='team[0].name')].message").value("필수 입력 항목입니다."));
    }

    @Test
    void 필드가_빠진_요청은_400이고_빈_배열은_통과한다() throws Exception {
        mockMvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"a\", \"team\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='history')]").exists());

        mockMvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"a\", \"history\": [], \"team\": [] }"))
                .andExpect(status().isOk());
    }

    @Test
    void 깨진_JSON은_400_VALIDATION_FAILED이고_fieldErrors는_생략된다() throws Exception {
        mockMvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void BusinessException은_ErrorCode의_상태와_필드_에러로_변환된다() throws Exception {
        mockMvc.perform(get("/sample/duplicated"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLUG_DUPLICATED"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 슬러그입니다."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("slug"));

        mockMvc.perform(get("/sample/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void 처리되지_않은_예외는_스택을_숨기고_500_INTERNAL_ERROR로_변환된다() throws Exception {
        mockMvc.perform(get("/sample/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("boom"))));
    }

    @Test
    void 프레임워크가_던지는_405도_공통_모양이다() throws Exception {
        mockMvc.perform(post("/sample/duplicated"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 슬러그_UNIQUE_위반은_409_SLUG_DUPLICATED로_변환된다() throws Exception {
        mockMvc.perform(get("/sample/slug-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLUG_DUPLICATED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("slug"));
    }

    @Test
    void 슬러그가_아닌_DB_제약_위반은_스택을_숨기고_500이다() throws Exception {
        mockMvc.perform(get("/sample/other-conflict"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("ck_member_status_role"))));
    }

    @Test
    void 존재하지_않는_enum_코드값은_400과_필드_경로를_반환한다() throws Exception {
        mockMvc.perform(post("/sample/category").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"category\": \"invalid\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("category"));
    }

    @RestController
    static class SampleController {

        @PostMapping("/sample")
        void create(@Valid @RequestBody SampleRequest request) {
        }

        @GetMapping("/sample/duplicated")
        void duplicated() {
            throw new BusinessException(ErrorCode.SLUG_DUPLICATED, "slug", ErrorCode.SLUG_DUPLICATED.message());
        }

        @GetMapping("/sample/not-found")
        void notFound() {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        @GetMapping("/sample/boom")
        void boom() {
            throw new IllegalStateException("boom");
        }

        @GetMapping("/sample/slug-conflict")
        void slugConflict() {
            throw new DataIntegrityViolationException("could not execute statement", new ConstraintViolationException(
                    "could not execute statement",
                    sqlException("duplicate key value violates unique constraint \"uk_project_slug\""),
                    "uk_project_slug"));
        }

        @GetMapping("/sample/other-conflict")
        void otherConflict() {
            throw new DataIntegrityViolationException("could not execute statement", new ConstraintViolationException(
                    "could not execute statement",
                    sqlException("new row violates check constraint \"ck_member_status_role\""),
                    "ck_member_status_role"));
        }

        @PostMapping("/sample/category")
        void category(@RequestBody CategoryRequest request) {
        }

        private static SQLException sqlException(String message) {
            return new SQLException(message);
        }
    }

    record SampleRequest(
            @NotBlank String title,
            @NotNull List<@Size(max = 4) String> history,
            @NotNull List<@Valid TeamRequest> team) {
    }

    record TeamRequest(@NotBlank String name) {
    }

    record CategoryRequest(SampleCategory category) {
    }

    enum SampleCategory {
        A, B;

        @JsonCreator
        static SampleCategory fromCode(String code) {
            return switch (code) {
                case "a" -> A;
                case "b" -> B;
                default -> throw new IllegalArgumentException("Unknown category: " + code);
            };
        }
    }
}
