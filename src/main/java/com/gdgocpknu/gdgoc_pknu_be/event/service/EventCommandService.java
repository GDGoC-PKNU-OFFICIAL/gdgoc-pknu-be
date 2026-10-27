package com.gdgocpknu.gdgoc_pknu_be.event.service;

import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.Event;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventRepository;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventOrderRequest;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventRequest;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventResponse;
import com.gdgocpknu.gdgoc_pknu_be.event.mapper.EventMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 등록 · 수정 · 삭제 · 같은 날짜 순서 변경(A-31 ~ A-33). `sortOrder` 규칙(BACK_ARCHITECTURE 5-6):
 * 등록은 해당 날짜의 마지막 순서 + 1, 날짜 변경은 새 날짜의 맨 뒤, 삭제는 재정렬하지 않음, 순서 변경은 0부터 재부여.
 * 수정은 내용이 같아도 `updatedAt`을 갱신하고(Project · Study와 같음), 순서 변경은 갱신하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EventCommandService {

    private static final String ORDER_MISMATCH_MESSAGE = "해당 날짜의 일정을 빠짐없이 한 번씩 보내 주세요.";

    private final EventRepository eventRepository;
    private final EventQueryService eventQueryService;
    private final EventMapper eventMapper;
    private final Clock clock;

    public EventResponse create(EventRequest request) {
        Event event = Event.builder()
                .title(request.title())
                .category(request.category())
                .date(request.date())
                .endDate(request.endDate())
                .time(request.time())
                .place(request.place())
                .summary(request.summary())
                .sortOrder(nextSortOrder(request.date()))
                .build();

        return eventMapper.toResponse(eventRepository.save(event));
    }

    /** 전체 필드 교체. `date`가 바뀌면 새 날짜의 마지막 순서로 옮기고, 그대로면 순서를 유지한다. */
    public EventResponse update(Long id, EventRequest request) {
        Event event = eventQueryService.getOrThrow(id);
        boolean dateChanged = !event.getDate().equals(request.date());
        // 엔티티를 바꾸기 전에 구한다. 바꾼 뒤에 조회하면 auto flush로 자기 자신이 새 날짜의 마지막 순서에 잡힌다.
        Integer movedSortOrder = dateChanged ? nextSortOrder(request.date()) : null;

        event.updateContent(request.title(), request.category(), request.date(), request.endDate(),
                request.time(), request.place(), request.summary());
        if (movedSortOrder != null) {
            event.changeSortOrder(movedSortOrder);
        }
        // 내용이 같은 PUT은 행이 dirty하지 않아 auditing이 updatedAt을 갱신하지 않는다. 저장 요청이면 항상 갱신한다.
        event.touch(clock.instant());

        // updatedAt(@LastModifiedDate)은 flush 시점에 채워지므로, 응답을 만들기 전에 flush해야 새 값이 나간다.
        eventRepository.flush();
        return eventMapper.toResponse(event);
    }

    /** 같은 날짜의 남은 일정 순서는 그대로 둔다. 상대 순서만 유지되면 되므로 번호를 다시 매기지 않는다(명세 2-6). */
    public void delete(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        eventRepository.deleteById(id);
    }

    /**
     * `orderedIds`가 해당 날짜의 일정 id 전체와 정확히 일치해야 한다(명세 4-5). 누락 · 다른 날짜 id · 없는 id는 400,
     * 중복은 요청 검증(`@NoDuplicates`)이 먼저 막는다. 일치하면 보낸 순서대로 `sortOrder`를 0부터 다시 매긴다.
     * 벌크 UPDATE로 바꾸므로 `updatedAt`은 그대로다 — 순서만 바꿔도 대시보드 "최근 수정"이 채워지지 않게 한다(명세 3-6).
     */
    public void reorder(EventOrderRequest request) {
        List<Event> events = eventRepository.findByDate(request.date());
        if (!new HashSet<>(request.orderedIds()).equals(events.stream().map(Event::getId).collect(Collectors.toSet()))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "orderedIds", ORDER_MISMATCH_MESSAGE);
        }

        List<Long> orderedIds = request.orderedIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            eventRepository.updateSortOrder(orderedIds.get(i), i);
        }
    }

    private int nextSortOrder(LocalDate date) {
        return eventRepository.findFirstByDateOrderBySortOrderDesc(date)
                .map(last -> last.getSortOrder() + 1)
                .orElse(0);
    }
}
