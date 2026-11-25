package com.gdgocpknu.gdgoc_pknu_be.member.dto;

import com.gdgocpknu.gdgoc_pknu_be.common.validation.NoDuplicates;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 멤버 표시 순서 바꾸기 (PUT /api/admin/members/order). 한 구분(코어멤버 또는 시니어)의 멤버 id 를
 * 보일 순서대로 빠짐없이 보낸다. 그 구분의 멤버와 하나라도 다르면(누가 그사이 추가 · 삭제됨) 400 이다.
 *
 * @param status 순서를 바꿀 구분
 * @param ids    그 구분의 멤버 id 전부, 앞에 보일 사람부터
 */
public record MemberOrderRequest(
        @NotNull MemberStatus status,
        @NotNull @Size(min = 1, max = 500) @NoDuplicates List<@NotNull Long> ids) {
}
