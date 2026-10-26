package com.gdgocpknu.gdgoc_pknu_be.study.domain;

import com.gdgocpknu.gdgoc_pknu_be.common.support.LikePattern;
import com.gdgocpknu.gdgoc_pknu_be.common.support.PeriodStatus;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/**
 * 관리자 목록(A-21)의 동적 조건 (API 명세서 2-5). 프로젝트와 같은 규칙이며, 검색 대상만 제목 · 리더다.
 * 기간 조건은 인덱스를 탈 수 있게 컬럼에 함수를 씌우지 않고 날짜 범위로 비교한다.
 */
public final class StudySpecifications {

    private StudySpecifications() {
    }

    /** `q` — 제목 또는 리더 부분 일치, 대소문자 무시. `%` · `_`는 글자 그대로 찾는다. */
    public static Specification<Study> titleOrLeaderContains(String keyword) {
        String pattern = LikePattern.containsIgnoreCase(keyword);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, LikePattern.ESCAPE),
                cb.like(cb.lower(root.get("leader")), pattern, LikePattern.ESCAPE));
    }

    /** `status` — {@link PeriodStatus#of}와 같은 규칙을 SQL로 옮긴 것(저장하지 않는 계산값). */
    public static Specification<Study> status(PeriodStatus status, LocalDate firstDayOfThisMonth) {
        return (root, query, cb) -> switch (status) {
            case CURRENT -> cb.or(
                    cb.isNull(root.get("periodEnd")),
                    cb.greaterThanOrEqualTo(root.get("periodEnd"), firstDayOfThisMonth));
            case PAST -> cb.lessThan(root.get("periodEnd"), firstDayOfThisMonth);
        };
    }

    /** `year` — `period.start`의 연도. `[year-01-01, year+1-01-01)` 범위로 비교한다. */
    public static Specification<Study> startYear(int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate until = from.plusYears(1);
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("periodStart"), from),
                cb.lessThan(root.get("periodStart"), until));
    }
}
