package com.gdgocpknu.gdgoc_pknu_be.event.domain;

import com.gdgocpknu.gdgoc_pknu_be.common.support.LikePattern;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/**
 * 관리자 목록(A-31)의 동적 조건 (API 명세서 2-6). 프로젝트와 같은 규칙이며, 검색 대상은 제목, 연도 기준은 `date`다.
 * 기간 조건은 인덱스를 탈 수 있게 컬럼에 함수를 씌우지 않고 날짜 범위로 비교한다.
 */
public final class EventSpecifications {

    private EventSpecifications() {
    }

    /** `q` — 제목 부분 일치, 대소문자 무시. `%` · `_`는 글자 그대로 찾는다. */
    public static Specification<Event> titleContains(String keyword) {
        String pattern = LikePattern.containsIgnoreCase(keyword);
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, LikePattern.ESCAPE);
    }

    public static Specification<Event> category(EventCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    /** `year` — `date`(시작일)의 연도. `[year-01-01, year+1-01-01)` 범위로 비교한다. */
    public static Specification<Event> dateYear(int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate until = from.plusYears(1);
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("date"), from),
                cb.lessThan(root.get("date"), until));
    }
}
