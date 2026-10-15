package com.gdgocpknu.gdgoc_pknu_be.study.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudyRequest.OutputRequest;
import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudyRequest.PeriodRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Bean Validation만 단독으로 돌려 API 명세서 4-3 규칙을 확인한다. Spring 컨텍스트·DB가 필요 없다. */
class StudyRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void 모든_필드를_채우면_통과한다() {
        assertThat(validator.validate(valid(r -> r))).isEmpty();
    }

    @Test
    void 선택_필드인_format과_period_end는_없어도_통과한다() {
        StudyRequest request = valid(r -> new StudyRequest(r.title(), r.slug(), r.summary(), r.topic(),
                new PeriodRequest("2026.03", null), r.leader(), r.participants(), null, r.outputs()));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 빈_배열은_통과하지만_배열_필드_자체가_없으면_위반이다() {
        StudyRequest emptyArrays = valid(r -> new StudyRequest(r.title(), r.slug(), r.summary(), r.topic(),
                r.period(), r.leader(), List.of(), r.format(), List.of()));
        assertThat(fieldsOf(validator.validate(emptyArrays))).isEmpty();

        StudyRequest missingArrays = valid(r -> new StudyRequest(r.title(), r.slug(), r.summary(), r.topic(),
                r.period(), r.leader(), null, r.format(), null));
        assertThat(fieldsOf(validator.validate(missingArrays))).contains("participants", "outputs");
    }

    @Test
    void 종료월이_시작월보다_빠르면_period_end에_위반이_달린다() {
        StudyRequest request = valid(r -> new StudyRequest(r.title(), r.slug(), r.summary(), r.topic(),
                new PeriodRequest("2026.09", "2026.03"), r.leader(), r.participants(), r.format(), r.outputs()));

        assertThat(fieldsOf(validator.validate(request))).contains("period.end");
    }

    @Test
    void 형식이_깨진_기간은_순서_검사_없이_형식_위반만_난다() {
        StudyRequest request = valid(r -> new StudyRequest(r.title(), r.slug(), r.summary(), r.topic(),
                new PeriodRequest("2026-03", "2026.13"), r.leader(), r.participants(), r.format(), r.outputs()));

        assertThat(fieldsOf(validator.validate(request))).containsExactlyInAnyOrder("period.start", "period.end");
    }

    @Test
    void 슬러그_형식이_어긋나면_위반이다() {
        StudyRequest request = valid(r -> new StudyRequest(r.title(), "Spring Study", r.summary(), r.topic(),
                r.period(), r.leader(), r.participants(), r.format(), r.outputs()));

        assertThat(fieldsOf(validator.validate(request))).contains("slug");
    }

    @Test
    void 참여자는_30명까지이고_이름이_비어_있거나_null이면_위반이다() {
        List<String> thirtyOne = IntStream.range(0, 31).mapToObj(i -> "참여자" + i).toList();
        StudyRequest tooMany = valid(r -> new StudyRequest(r.title(), r.slug(), r.summary(), r.topic(),
                r.period(), r.leader(), thirtyOne, r.format(), r.outputs()));
        assertThat(fieldsOf(validator.validate(tooMany))).contains("participants");

        List<String> withBlankAndNull = new ArrayList<>(List.of("  "));
        withBlankAndNull.add(null);
        StudyRequest badNames = valid(r -> new StudyRequest(r.title(), r.slug(), r.summary(), r.topic(),
                r.period(), r.leader(), withBlankAndNull, r.format(), r.outputs()));
        assertThat(fieldsOf(validator.validate(badNames)))
                .contains("participants[0].<list element>", "participants[1].<list element>");
    }

    @Test
    void 산출물은_5개까지이고_label_url은_필수이며_url은_http_s여야_한다() {
        List<OutputRequest> six = IntStream.range(0, 6)
                .mapToObj(i -> new OutputRequest("자료" + i, "https://example.com/" + i)).toList();
        StudyRequest tooMany = valid(r -> new StudyRequest(r.title(), r.slug(), r.summary(), r.topic(),
                r.period(), r.leader(), r.participants(), r.format(), six));
        assertThat(fieldsOf(validator.validate(tooMany))).contains("outputs");

        StudyRequest bad = valid(r -> new StudyRequest(r.title(), r.slug(), r.summary(), r.topic(),
                r.period(), r.leader(), r.participants(), r.format(),
                List.of(new OutputRequest("", "ftp://example.com"), new OutputRequest("라벨", null))));
        assertThat(fieldsOf(validator.validate(bad)))
                .contains("outputs[0].label", "outputs[0].url", "outputs[1].url");
    }

    @Test
    void 산출물_배열에_null_원소가_있으면_위반이다() {
        List<OutputRequest> withNull = new ArrayList<>();
        withNull.add(null);
        StudyRequest request = valid(r -> new StudyRequest(r.title(), r.slug(), r.summary(), r.topic(),
                r.period(), r.leader(), r.participants(), r.format(), withNull));

        assertThat(fieldsOf(validator.validate(request))).contains("outputs[0].<list element>");
    }

    private static Set<String> fieldsOf(Set<ConstraintViolation<StudyRequest>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    /** 전부 유효한 요청을 만든 뒤 customizer로 필드를 바꿔 개별 규칙을 검증한다. */
    private static StudyRequest valid(UnaryOperator<StudyRequest> customizer) {
        return customizer.apply(new StudyRequest(
                "스프링 스터디", "spring-study", "스프링 부트 기초부터 JPA까지", "Backend",
                new PeriodRequest("2026.03", "2026.06"), "홍길동",
                List.of("김부경", "이서울"), "주 1회 온라인",
                List.of(new OutputRequest("회고록", "https://velog.io/@gdgoc/retrospective"))));
    }
}
