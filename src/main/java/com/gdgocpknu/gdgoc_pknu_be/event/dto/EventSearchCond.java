package com.gdgocpknu.gdgoc_pknu_be.event.dto;

import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventCategory;

/**
 * 관리자 목록(A-31) 필터 (API 명세서 2-6). 모든 값은 선택이며, `null`이면 해당 조건을 걸지 않는다.
 *
 * @param q        제목 검색어. 앞뒤 공백을 제거하고, 비어 있으면 검색하지 않는다
 * @param category 일정 유형 코드값
 * @param year     date(시작일)의 연도
 */
public record EventSearchCond(String q, EventCategory category, Integer year) {

    public EventSearchCond {
        q = (q == null || q.isBlank()) ? null : q.strip();
    }
}
