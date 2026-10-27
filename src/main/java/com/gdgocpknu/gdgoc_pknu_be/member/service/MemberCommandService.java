package com.gdgocpknu.gdgoc_pknu_be.member.service;

import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.Member;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRepository;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.AdminMemberResponse;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberRequest;
import com.gdgocpknu.gdgoc_pknu_be.member.mapper.MemberMapper;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 등록 · 수정 · 삭제(A-42, A-43, 목록 삭제). 공개 동의 확인 시각(`consentConfirmedAt`)은 등록 때 한 번 기록하고
 * 수정에서는 바꾸지 않는다(명세 2-7). 삭제해도 프로젝트 · 스터디 기록은 영향이 없다(참여자는 이름 문자열).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandService {

    private final MemberRepository memberRepository;
    private final MemberQueryService memberQueryService;
    private final MemberMapper memberMapper;
    private final Clock clock;

    public AdminMemberResponse create(MemberRequest request) {
        Member member = Member.builder()
                .status(request.status())
                .role(request.role())
                .name(request.name())
                .photoUrl(request.photoUrl())
                .department(request.department())
                .history(request.history())
                .interests(request.interests())
                .part(request.part())
                .bio(request.bio())
                .githubUrl(request.links().github())
                .homepageUrl(request.links().homepage())
                .consentConfirmedAt(clock.instant())
                .build();

        return memberMapper.toAdminResponse(memberRepository.save(member));
    }

    /** 전체 필드 교체. 시니어로 전환하면 `role`이 `null`이어야 하며 요청 검증이 먼저 막는다. */
    public AdminMemberResponse update(Long id, MemberRequest request) {
        Member member = memberQueryService.getOrThrow(id);

        member.updateContent(
                request.status(), request.role(), request.name(), request.photoUrl(), request.department(),
                request.history(), request.interests(), request.part(), request.bio(),
                request.links().github(), request.links().homepage());
        // 내용이 같은 PUT은 행이 dirty하지 않아 auditing이 updatedAt을 갱신하지 않는다. 저장 요청이면 항상 갱신한다.
        member.touch(clock.instant());

        // updatedAt(@LastModifiedDate)은 flush 시점에 채워지므로, 응답을 만들기 전에 flush해야 새 값이 나간다.
        memberRepository.flush();
        return memberMapper.toAdminResponse(member);
    }

    public void delete(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        memberRepository.deleteById(id);
    }
}
