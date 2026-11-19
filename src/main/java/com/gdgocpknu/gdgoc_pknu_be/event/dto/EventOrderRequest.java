package com.gdgocpknu.gdgoc_pknu_be.event.dto;

import com.gdgocpknu.gdgoc_pknu_be.common.validation.NoDuplicates;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * API 명세서 4-5 EventOrderRequest. `orderedIds`가 해당 날짜의 일정 id 전체와 정확히 일치하는지는
 * DB를 봐야 알 수 있으므로 서비스가 검사한다. 여기서는 형태(누락 · null 원소 · 중복)만 본다.
 */
public record EventOrderRequest(
        @NotNull LocalDate date,
        @NotNull @Size(max = EventOrderRequest.MAX_ORDERED_IDS) @NoDuplicates List<@NotNull Long> orderedIds) {

    /** 같은 날짜에 일정이 이보다 많을 일은 없다. 목록 크기를 DB 조회 전에 제한한다. */
    public static final int MAX_ORDERED_IDS = 100;
}
