package com.gdgocpknu.gdgoc_pknu_be.member.dto;

import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRole;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;

/**
 * 관리자 목록(A-41) 필터 (API 명세서 2-7). 모든 값은 선택이며, `null`이면 해당 조건을 걸지 않는다.
 *
 * @param q      이름 검색어. 앞뒤 공백을 제거하고, 비어 있으면 검색하지 않는다
 * @param status core | senior
 * @param role   lead | part-lead | core
 */
public record MemberSearchCond(String q, MemberStatus status, MemberRole role) {

    public MemberSearchCond {
        q = (q == null || q.isBlank()) ? null : q.strip();
    }
}
