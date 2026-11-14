package com.gdgocpknu.gdgoc_pknu_be.setting.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gdgocpknu.gdgoc_pknu_be.common.support.StrictIntegerDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * API 명세서 4-7 SettingRequest. 단일 레코드를 한 번에 교체한다(PUT은 전체 필드 교체).
 * 모집 기간은 비우려면 `recruitPeriod: null`을 보낸다. 통계는 순서가 곧 표시 순서이며 정확히 4개다.
 */
public record SettingRequest(
        @NotNull Boolean recruiting,
        @NotBlank @Size(max = 60) String bannerText,
        @Valid RecruitPeriodRequest recruitPeriod,
        @NotNull @Size(min = 4, max = 4, message = "통계는 정확히 4개여야 합니다.")
        List<@NotNull @Valid StatRequest> stats) {

    /** 날짜는 `YYYY-MM-DD`이며 Jackson이 `LocalDate`로 읽는다. 형식이 틀리거나 없는 날짜는 역직렬화 단계에서 400이다. */
    @ValidRecruitPeriod
    public record RecruitPeriodRequest(LocalDate start, LocalDate end) {
    }

    /** 통계 값의 상한. 홈 화면의 큰 숫자 칸에 들어가는 자릿수(쉼표 포함 7자)에 맞춘 값이다. */
    public static final int MAX_STAT_VALUE = 999_999;

    /**
     * `value`는 0 이상 {@value #MAX_STAT_VALUE} 이하 정수다. 소수 · 문자열이 조용히 정수로 바뀌어 저장되지 않도록
     * 정수만 받는다. 상한이 없으면 `int` 최댓값(21억)까지 들어가 홈 화면 레이아웃이 깨진다.
     */
    public record StatRequest(
            @NotBlank @Size(max = 20) String label,
            @NotNull @PositiveOrZero @Max(value = MAX_STAT_VALUE, message = "999,999 이하여야 합니다.")
            @JsonDeserialize(using = StrictIntegerDeserializer.class) Integer value,
            @Size(max = 10) String unit,
            @Size(max = 5) String suffix) {
    }
}
