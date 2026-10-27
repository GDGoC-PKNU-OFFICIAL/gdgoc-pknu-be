package com.gdgocpknu.gdgoc_pknu_be.member.dto;

import com.gdgocpknu.gdgoc_pknu_be.common.validation.NoDuplicates;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberPart;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRole;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * API 명세서 4-6 MemberRequest. 등록 · 수정 공통 요청이다(PUT은 전체 필드 교체).
 * 배열은 `@NotNull`로 받아 "필드 누락(400)"과 "빈 배열([])"을 구분하고(1-4), 원소의 `null` · 빈 문자열은 막는다.
 * `consentConfirmed`는 저장하지 않는다 — 반드시 `true`여야 한다는 검증용 값이며, 시각은 서버가 기록한다.
 * `photoUrl`의 R2 접두사 · 객체 재검증은 업로드 기능(Phase 4)에서 더한다. 여기서는 URL 형식만 본다.
 */
@RoleMatchesStatus
public record MemberRequest(
        @NotNull MemberStatus status,
        MemberRole role,
        @NotBlank @Size(max = 20) String name,
        @Pattern(regexp = "^https?://.+", message = "URL 형식이 올바르지 않습니다.") @Size(max = 500) String photoUrl,
        @NotBlank @Size(max = 30) String department,
        @NotNull @Size(max = 2) List<@NotBlank @Size(max = 40) String> history,
        @NotNull @Size(max = 3) @NoDuplicates List<@NotBlank @Size(max = 15) String> interests,
        @NotNull MemberPart part,
        @NotBlank @Size(max = 60) String bio,
        @NotNull @Valid LinksRequest links,
        @NotNull @AssertTrue(message = "공개 동의를 확인해야 합니다.") Boolean consentConfirmed) {

    public record LinksRequest(
            @Pattern(regexp = "^https?://.+", message = "URL 형식이 올바르지 않습니다.") @Size(max = 500) String github,
            @Pattern(regexp = "^https?://.+", message = "URL 형식이 올바르지 않습니다.") @Size(max = 500) String homepage) {
    }
}
