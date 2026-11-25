package com.gdgocpknu.gdgoc_pknu_be.member.domain;

import com.gdgocpknu.gdgoc_pknu_be.common.support.LikePattern;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/** 멤버 목록의 동적 조건과 기본 정렬 (API 명세서 2-7). */
public final class MemberSpecifications {

    private MemberSpecifications() {
    }

    /** `q` — 이름 부분 일치, 대소문자 무시. `%` · `_`는 글자 그대로 찾는다. */
    public static Specification<Member> nameContains(String keyword) {
        String pattern = LikePattern.containsIgnoreCase(keyword);
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, LikePattern.ESCAPE);
    }

    public static Specification<Member> status(MemberStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * 기본 정렬: 구분(코어멤버 먼저) → 관리자가 정한 표시 순서(sortOrder) → id.
     * 구분은 저장된 코드값의 사전순(`core` &lt; `senior`)에 기대지 않도록 CASE로 순위를 매긴다.
     * Sort로는 표현할 수 없어 정렬을 쿼리에 직접 건다 — 건수 조회(count)에는 정렬을 걸지 않는다.
     * 호출하는 쪽은 {@code Sort.unsorted()}를 써야 한다. 정렬이 있으면 Spring Data가 이 정렬을 덮어쓴다.
     */
    public static Specification<Member> defaultOrder() {
        return (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                query.orderBy(
                        cb.asc(statusRank(root, cb)),
                        cb.asc(root.get("sortOrder")),
                        cb.asc(root.get("id")));
            }
            return cb.conjunction();
        };
    }

    private static Expression<Integer> statusRank(Root<Member> root, CriteriaBuilder cb) {
        return cb.<Integer>selectCase()
                .when(cb.equal(root.get("status"), MemberStatus.CORE), 0)
                .otherwise(1);
    }
}
