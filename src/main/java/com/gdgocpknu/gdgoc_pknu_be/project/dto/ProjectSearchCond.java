package com.gdgocpknu.gdgoc_pknu_be.project.dto;

import com.gdgocpknu.gdgoc_pknu_be.common.support.PeriodStatus;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectCategory;

/**
 * 관리자 목록(A-11) 필터 (API 명세서 2-4). 모든 값은 선택이며, `null`이면 해당 조건을 걸지 않는다.
 *
 * @param q        제목 검색어. 앞뒤 공백을 제거하고, 비어 있으면 검색하지 않는다
 * @param status   current | past (period.end로 계산한 값 기준)
 * @param category 유형 코드값
 * @param year     period.start의 연도
 */
public record ProjectSearchCond(String q, PeriodStatus status, ProjectCategory category, Integer year) {

    public ProjectSearchCond {
        q = (q == null || q.isBlank()) ? null : q.strip();
    }
}
