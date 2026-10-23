package com.gdgocpknu.gdgoc_pknu_be.study.dto;

import com.gdgocpknu.gdgoc_pknu_be.common.support.PeriodStatus;

/**
 * 관리자 목록(A-21) 필터 (API 명세서 2-5). 모든 값은 선택이며, `null`이면 해당 조건을 걸지 않는다.
 *
 * @param q      제목 · 리더 검색어. 앞뒤 공백을 제거하고, 비어 있으면 검색하지 않는다
 * @param status current | past (period.end로 계산한 값 기준)
 * @param year   period.start의 연도
 */
public record StudySearchCond(String q, PeriodStatus status, Integer year) {

    public StudySearchCond {
        q = (q == null || q.isBlank()) ? null : q.strip();
    }
}
