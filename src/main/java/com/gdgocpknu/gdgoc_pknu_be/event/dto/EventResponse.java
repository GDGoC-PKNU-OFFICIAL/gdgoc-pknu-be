package com.gdgocpknu.gdgoc_pknu_be.event.dto;

import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventCategory;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * API 명세서 3-3 EventItem. 공개 · 관리자 응답이 같은 모양이다.
 * 날짜는 `YYYY-MM-DD`로 직렬화되고, 값이 없는 선택 필드(endDate, time, place, summary)는
 * 전역 설정(non_null)으로 응답에서 생략된다.
 */
public record EventResponse(
        Long id,
        String title,
        EventCategory category,
        LocalDate date,
        LocalDate endDate,
        String time,
        String place,
        String summary,
        int sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
