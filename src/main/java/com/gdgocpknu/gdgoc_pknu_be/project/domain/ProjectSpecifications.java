package com.gdgocpknu.gdgoc_pknu_be.project.domain;

import com.gdgocpknu.gdgoc_pknu_be.common.support.LikePattern;
import com.gdgocpknu.gdgoc_pknu_be.common.support.PeriodStatus;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/**
 * 관리자 목록(A-11)의 동적 조건. 조건 하나당 메서드 하나로 두고, 어떤 조건을 켤지는 ProjectQueryService가 정한다.
 * 기간 조건은 인덱스를 탈 수 있게 컬럼에 함수를 씌우지 않고 날짜 범위로 비교한다.
 */
public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    /** `q` — 제목 부분 일치, 대소문자 무시. `%` · `_`는 글자 그대로 찾는다. */
    public static Specification<Project> titleContains(String keyword) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("title")), LikePattern.containsIgnoreCase(keyword), LikePattern.ESCAPE);
    }

    /**
     * `status` — 저장하지 않는 계산값이므로 {@link PeriodStatus#of}와 같은 규칙을 SQL로 옮긴다.
     * current: `period_end IS NULL OR period_end >= 이번 달 1일`, past: `period_end < 이번 달 1일`.
     */
    public static Specification<Project> status(PeriodStatus status, LocalDate firstDayOfThisMonth) {
        return (root, query, cb) -> switch (status) {
            case CURRENT -> cb.or(
                    cb.isNull(root.get("periodEnd")),
                    cb.greaterThanOrEqualTo(root.get("periodEnd"), firstDayOfThisMonth));
            case PAST -> cb.lessThan(root.get("periodEnd"), firstDayOfThisMonth);
        };
    }

    public static Specification<Project> category(ProjectCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    /** `year` — `period.start`의 연도. `[year-01-01, year+1-01-01)` 범위로 비교한다. */
    public static Specification<Project> startYear(int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate until = from.plusYears(1);
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("periodStart"), from),
                cb.lessThan(root.get("periodStart"), until));
    }
}
