package com.gdgocpknu.gdgoc_pknu_be.member.mapper;

import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.Member;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.AdminMemberResponse;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Entity → 응답 DTO. 공개 응답에는 공개 동의 확인 일시를 싣지 않는다(명세 3-4). */
@Component
@RequiredArgsConstructor
public class MemberMapper {

    private final KstDates kstDates;

    public MemberResponse toPublicResponse(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getStatus(),
                member.getRole(),
                member.getName(),
                member.getPhotoUrl(),
                member.getDepartment(),
                member.getHistory(),
                member.getInterests(),
                member.getPart(),
                member.getBio(),
                links(member),
                kstDates.toKst(member.getCreatedAt()),
                kstDates.toKst(member.getUpdatedAt()));
    }

    public List<MemberResponse> toPublicResponses(List<Member> members) {
        return members.stream().map(this::toPublicResponse).toList();
    }

    public AdminMemberResponse toAdminResponse(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getStatus(),
                member.getRole(),
                member.getName(),
                member.getPhotoUrl(),
                member.getDepartment(),
                member.getHistory(),
                member.getInterests(),
                member.getPart(),
                member.getBio(),
                links(member),
                kstDates.toKst(member.getConsentConfirmedAt()),
                kstDates.toKst(member.getCreatedAt()),
                kstDates.toKst(member.getUpdatedAt()));
    }

    private static MemberResponse.Links links(Member member) {
        return new MemberResponse.Links(member.getGithubUrl(), member.getHomepageUrl());
    }
}
