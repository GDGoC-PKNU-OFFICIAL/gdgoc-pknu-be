package com.gdgocpknu.gdgoc_pknu_be.event.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Bean Validation만 단독으로 돌려 API 명세서 4-4 · 4-5 규칙을 확인한다. Spring 컨텍스트·DB가 필요 없다. */
class EventRequestValidationTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 20);

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void 모든_필드를_채우면_통과한다() {
        assertThat(validator.validate(request(DATE, DATE.plusDays(1)))).isEmpty();
    }

    @Test
    void 선택_필드는_모두_없어도_통과한다() {
        EventRequest request = new EventRequest("정기 모임", EventCategory.MEETING, DATE, null, null, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 필수_필드가_없으면_각_필드에_위반이_달린다() {
        EventRequest request = new EventRequest(" ", null, null, null, null, null, null);

        assertThat(fieldsOf(validator.validate(request))).contains("title", "category", "date");
    }

    @Test
    void 종료일이_시작일보다_빠르면_endDate에_위반이_달린다() {
        assertThat(fieldsOf(validator.validate(request(DATE, DATE.minusDays(1))))).containsExactly("endDate");
    }

    @Test
    void 종료일이_시작일과_같으면_통과한다() {
        assertThat(validator.validate(request(DATE, DATE))).isEmpty();
    }

    @Test
    void 글자_수_제한을_넘으면_위반이다() {
        EventRequest request = new EventRequest(
                "가".repeat(41), EventCategory.SEMINAR, DATE, null, "1".repeat(21), "가".repeat(41), "가".repeat(81));

        assertThat(fieldsOf(validator.validate(request))).contains("title", "time", "place", "summary");
    }

    @Test
    void 글자_수_최대값은_통과한다() {
        EventRequest request = new EventRequest(
                "가".repeat(40), EventCategory.SEMINAR, DATE, null, "1".repeat(20), "가".repeat(40), "가".repeat(80));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 순서_변경_요청은_날짜와_id_목록이_필수다() {
        assertThat(fieldsOf(validator.validate(new EventOrderRequest(null, null)))).contains("date", "orderedIds");
    }

    @Test
    void 순서_변경_요청의_빈_목록은_형태상_통과한다() {
        // 그 날짜에 일정이 하나도 없을 때만 의미가 있다. 일치 여부는 서비스가 DB를 보고 판단한다.
        assertThat(validator.validate(new EventOrderRequest(DATE, List.of()))).isEmpty();
    }

    @Test
    void 순서_변경_요청에_중복_id나_null_원소가_있으면_위반이다() {
        assertThat(fieldsOf(validator.validate(new EventOrderRequest(DATE, List.of(3L, 1L, 3L)))))
                .contains("orderedIds");
        assertThat(fieldsOf(validator.validate(new EventOrderRequest(DATE, Arrays.asList(1L, null)))))
                .anyMatch(field -> field.startsWith("orderedIds[1]"));
    }

    private static EventRequest request(LocalDate date, LocalDate endDate) {
        return new EventRequest("GDGoC 세미나", EventCategory.SEMINAR, date, endDate, "19:00–21:00", "공학관 101호", "한 줄 요약");
    }

    private static Set<String> fieldsOf(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }
}
