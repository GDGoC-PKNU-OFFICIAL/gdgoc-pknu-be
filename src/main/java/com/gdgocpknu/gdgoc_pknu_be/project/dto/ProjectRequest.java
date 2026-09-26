package com.gdgocpknu.gdgoc_pknu_be.project.dto;

import com.gdgocpknu.gdgoc_pknu_be.common.validation.HasYearMonthRange;
import com.gdgocpknu.gdgoc_pknu_be.common.validation.NoDuplicates;
import com.gdgocpknu.gdgoc_pknu_be.common.validation.PeriodOrder;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * API 명세서 4-2 ProjectRequest. 등록 · 수정 공통 요청이다(PUT은 전체 필드 교체).
 * 선택 필드라도 배열은 `@NotNull`로 받아 "필드 누락(400)"과 "빈 배열([])"을 구분한다(1-4).
 */
public record ProjectRequest(
        @NotBlank @Size(max = 40) String title,
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "영문 소문자·숫자·하이픈만 사용할 수 있습니다.")
        @Size(max = 60)
        String slug,
        @NotBlank @Size(max = 80) String summary,
        @NotNull ProjectCategory category,
        @NotNull @Valid PeriodRequest period,
        @Pattern(regexp = "^https?://.+", message = "URL 형식이 올바르지 않습니다.") @Size(max = 500) String thumbnailUrl,
        @NotNull @Size(min = 1, max = 10) List<@Size(max = 1000) String> description,
        @NotNull @Size(max = 20) List<@Valid TeamMemberRequest> team,
        @NotNull @Size(max = 10) @NoDuplicates List<@Size(max = 20) String> techStack,
        @NotNull @Size(max = 10) List<@Size(max = 80) String> features,
        @NotNull @Size(max = 10) List<@Size(max = 80) String> outcomes,
        @NotNull @Valid LinksRequest links) {

    private static final String YEAR_MONTH_PATTERN = "^\\d{4}\\.(0[1-9]|1[0-2])$";

    /** end가 없으면 진행 중(4-2). 형식은 `@Pattern`, 순서(start ≤ end)는 `@PeriodOrder`가 검사한다. */
    @PeriodOrder
    public record PeriodRequest(
            @NotBlank @Pattern(regexp = YEAR_MONTH_PATTERN, message = "YYYY.MM 형식이어야 합니다.") String start,
            @Pattern(regexp = YEAR_MONTH_PATTERN, message = "YYYY.MM 형식이어야 합니다.") String end)
            implements HasYearMonthRange {
    }

    public record TeamMemberRequest(
            @NotBlank @Size(max = 20) String name,
            @NotBlank @Size(max = 20) String role) {
    }

    public record LinksRequest(
            @Pattern(regexp = "^https?://.+", message = "URL 형식이 올바르지 않습니다.") @Size(max = 500) String github,
            @Pattern(regexp = "^https?://.+", message = "URL 형식이 올바르지 않습니다.") @Size(max = 500) String demo) {
    }
}
