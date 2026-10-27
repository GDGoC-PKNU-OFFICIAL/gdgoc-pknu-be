package com.gdgocpknu.gdgoc_pknu_be.member.dto;

import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberPart;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRole;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * API 명세서 3-4 Member의 공개 응답. 공개 동의 확인 일시(`consentConfirmedAt`)는 포함하지 않는다 —
 * 관리자 응답은 {@link AdminMemberResponse}. 값이 없는 선택 필드(`role`, `photoUrl`, 링크)는 생략된다.
 */
public record MemberResponse(
        Long id,
        MemberStatus status,
        MemberRole role,
        String name,
        String photoUrl,
        String department,
        List<String> history,
        List<String> interests,
        MemberPart part,
        String bio,
        Links links,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public record Links(String github, String homepage) {
    }
}
