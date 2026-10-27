package com.gdgocpknu.gdgoc_pknu_be.event.dto;

import com.gdgocpknu.gdgoc_pknu_be.common.validation.DateOrder;
import com.gdgocpknu.gdgoc_pknu_be.common.validation.HasDateRange;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * API 명세서 4-4 EventRequest. 등록 · 수정 공통 요청이다(PUT은 전체 필드 교체).
 * 날짜는 `YYYY-MM-DD`이며 Jackson이 `LocalDate`로 읽는다. 형식이 틀리거나 없는 날짜(`2026-02-30`)는
 * 역직렬화 단계에서 400 + `fieldErrors[date]`가 된다. `sortOrder`는 받지 않는다(서버가 지정).
 */
@DateOrder
public record EventRequest(
        @NotBlank @Size(max = 40) String title,
        @NotNull EventCategory category,
        @NotNull LocalDate date,
        LocalDate endDate,
        @Size(max = 20) String time,
        @Size(max = 40) String place,
        @Size(max = 80) String summary) implements HasDateRange {
}
