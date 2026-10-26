package com.gdgocpknu.gdgoc_pknu_be.event.controller;

import com.gdgocpknu.gdgoc_pknu_be.common.dto.PageResponse;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventCategory;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventOrderRequest;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventRequest;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventResponse;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventSearchCond;
import com.gdgocpknu.gdgoc_pknu_be.event.service.EventCommandService;
import com.gdgocpknu.gdgoc_pknu_be.event.service.EventQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 CRUD + 같은 날짜 순서 변경(A-31 ~ A-33). 일정은 슬러그가 없어 slug-check가 없다.
 * `/order`가 `/{id}`에 먹히지 않도록 id는 숫자 패턴으로 선언한다(명세 2-6).
 */
@Validated
@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventQueryService eventQueryService;
    private final EventCommandService eventCommandService;

    /**
     * `category`는 코드값(`seminar`)으로 받는다. `sort`는 `필드,방향` 형식이며
     * 허용 필드는 `date` · `title` · `updatedAt` (API 명세서 1-6, 2-6).
     */
    @GetMapping
    public PageResponse<EventResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) @Positive @Max(9999) Integer year,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive @Max(200) int size,
            @RequestParam(defaultValue = "date,desc") String sort) {
        return eventQueryService.findAllForAdmin(new EventSearchCond(q, category, year), page, size, sort);
    }

    @GetMapping("/{id:\\d+}")
    public EventResponse get(@PathVariable Long id) {
        return eventQueryService.getForAdmin(id);
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
        EventResponse response = eventCommandService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/events/" + response.id())).body(response);
    }

    @PutMapping("/{id:\\d+}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return eventCommandService.update(id, request);
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/order")
    public ResponseEntity<Void> reorder(@Valid @RequestBody EventOrderRequest request) {
        eventCommandService.reorder(request);
        return ResponseEntity.noContent().build();
    }
}
