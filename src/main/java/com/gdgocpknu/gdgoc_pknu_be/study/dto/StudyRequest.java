package com.gdgocpknu.gdgoc_pknu_be.study.dto;

import com.gdgocpknu.gdgoc_pknu_be.common.validation.HasYearMonthRange;
import com.gdgocpknu.gdgoc_pknu_be.common.validation.PeriodOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * API 명세서 4-3 StudyRequest. 등록 · 수정 공통 요청이다(PUT은 전체 필드 교체).
 * 선택 필드라도 배열은 `@NotNull`로 받아 "필드 누락(400)"과 "빈 배열([])"을 구분하고(1-4),
 * 배열 원소의 `null` · 빈 문자열은 저장 · 응답 변환 중 NPE(500)가 되므로 원소에도 제약을 건다.
 */
public record StudyRequest(
        @NotBlank @Size(max = 40) String title,
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "영문 소문자·숫자·하이픈만 사용할 수 있습니다.")
        @Size(max = 60)
        String slug,
        @NotBlank @Size(max = 80) String summary,
        @NotBlank @Size(max = 20) String topic,
        @NotNull @Valid PeriodRequest period,
        @NotBlank @Size(max = 20) String leader,
        @NotNull @Size(max = 30) List<@NotBlank @Size(max = 20) String> participants,
        @Size(max = 30) String format,
        @NotNull @Size(max = 5) List<@NotNull @Valid OutputRequest> outputs) {

    private static final String YEAR_MONTH_PATTERN = "^\\d{4}\\.(0[1-9]|1[0-2])$";

    /** end가 없으면 진행 중(4-3). 형식은 `@Pattern`, 순서(start ≤ end)는 `@PeriodOrder`가 검사한다. */
    @PeriodOrder
    public record PeriodRequest(
            @NotBlank @Pattern(regexp = YEAR_MONTH_PATTERN, message = "YYYY.MM 형식이어야 합니다.") String start,
            @Pattern(regexp = YEAR_MONTH_PATTERN, message = "YYYY.MM 형식이어야 합니다.") String end)
            implements HasYearMonthRange {
    }

    /** label · url 모두 필수(명세 4-3). */
    public record OutputRequest(
            @NotBlank @Size(max = 30) String label,
            @NotBlank
            @Pattern(regexp = "^https?://.+", message = "URL 형식이 올바르지 않습니다.")
            @Size(max = 500)
            String url) {
    }
}
