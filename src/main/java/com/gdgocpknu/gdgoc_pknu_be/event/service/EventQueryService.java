package com.gdgocpknu.gdgoc_pknu_be.event.service;

import com.gdgocpknu.gdgoc_pknu_be.common.dto.PageResponse;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.common.support.SortWhitelist;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.Event;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventRepository;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventSpecifications;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventResponse;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventSearchCond;
import com.gdgocpknu.gdgoc_pknu_be.event.mapper.EventMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryService {

    /** 같은 날짜 안에서는 `sortOrder` 순, 순서가 같으면(동시 등록) 먼저 등록한 것 먼저. */
    private static final Sort WITHIN_DATE = Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"));

    /** 공개 목록 정렬: `date` 오름차순 → `sortOrder` 오름차순 (API 명세서 2-1). */
    private static final Sort PUBLIC_ORDER = Sort.by(Sort.Order.asc("date")).and(WITHIN_DATE);

    /** 관리자 목록 `sort` 허용 필드 (API 명세서 2-6). 기본값 `date,desc`는 컨트롤러가 채운다. */
    private static final SortWhitelist ADMIN_SORTABLE = SortWhitelist.of("date", "title", "updatedAt");

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    /** 지난 일정 포함 전체. 캘린더 월 이동 · 다가오는 일정 계산은 프론트에서 처리한다. */
    public List<EventResponse> findAllForPublic() {
        return eventMapper.toResponses(eventRepository.findAll(PUBLIC_ORDER));
    }

    /**
     * 관리자 목록(A-31). 어떤 정렬이든 끝에 같은 날짜 안의 순서(`sortOrder`, `id`)를 붙인다 —
     * 기본 정렬(`date,desc`)에서 같은 날짜가 `sortOrder` 오름차순으로 나오게 하고(명세 2-6),
     * 정렬 값이 같은 행이 페이지 경계에서 중복 · 누락되지 않게 한다.
     */
    public PageResponse<EventResponse> findAllForAdmin(EventSearchCond cond, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, ADMIN_SORTABLE.parse(sort).and(WITHIN_DATE));
        Page<Event> result = eventRepository.findAll(adminFilter(cond), pageable);
        return PageResponse.of(result, eventMapper::toResponse);
    }

    private static Specification<Event> adminFilter(EventSearchCond cond) {
        List<Specification<Event>> specs = new ArrayList<>();
        if (cond.q() != null) {
            specs.add(EventSpecifications.titleContains(cond.q()));
        }
        if (cond.category() != null) {
            specs.add(EventSpecifications.category(cond.category()));
        }
        if (cond.year() != null) {
            specs.add(EventSpecifications.dateYear(cond.year()));
        }
        return Specification.allOf(specs);
    }

    public EventResponse getForAdmin(Long id) {
        return eventMapper.toResponse(getOrThrow(id));
    }

    Event getOrThrow(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
