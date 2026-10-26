package com.gdgocpknu.gdgoc_pknu_be.event.mapper;

import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.Event;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Entity → 응답 DTO. 일정에는 저장하지 않는 계산값이 없다 — "다가오는 일정"은 프론트가,
 * 대시보드 건수는 DashboardService가 `(endDate ?? date) ≥ 오늘`로 계산한다(명세 2-1, 3-6).
 */
@Component
@RequiredArgsConstructor
public class EventMapper {

    private final KstDates kstDates;

    public EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getCategory(),
                event.getDate(),
                event.getEndDate(),
                event.getTime(),
                event.getPlace(),
                event.getSummary(),
                event.getSortOrder(),
                kstDates.toKst(event.getCreatedAt()),
                kstDates.toKst(event.getUpdatedAt()));
    }

    public List<EventResponse> toResponses(List<Event> events) {
        return events.stream().map(this::toResponse).toList();
    }
}
