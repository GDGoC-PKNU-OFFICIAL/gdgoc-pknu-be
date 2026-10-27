package com.gdgocpknu.gdgoc_pknu_be.member.dto;

import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberPart;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRole;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;
import java.time.OffsetDateTime;
import java.util.List;

/** 관리자 응답. 공개 응답({@link MemberResponse})에 공개 동의 확인 일시(`consentConfirmedAt`)가 더해진다. */
public record AdminMemberResponse(
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
        MemberResponse.Links links,
        OffsetDateTime consentConfirmedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
