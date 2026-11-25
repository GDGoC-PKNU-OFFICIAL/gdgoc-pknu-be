package com.gdgocpknu.gdgoc_pknu_be.member.service;

import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.Member;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRepository;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.AdminMemberResponse;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberOrderRequest;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberRequest;
import com.gdgocpknu.gdgoc_pknu_be.member.mapper.MemberMapper;
import com.gdgocpknu.gdgoc_pknu_be.storage.UploadTarget;
import com.gdgocpknu.gdgoc_pknu_be.storage.service.UploadVerifier;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 등록 · 수정 · 삭제(A-42, A-43, 목록 삭제). 공개 동의 확인 시각(`consentConfirmedAt`)은 등록 때 한 번 기록하고
 * 수정에서는 바꾸지 않는다(명세 2-7). 삭제해도 프로젝트 · 스터디 기록은 영향이 없다(참여자는 이름 문자열).
 *
 * 표시 순서(sortOrder)는 구분마다 따로 매긴다. 새로 등록하거나 구분이 바뀐 멤버는 그 구분의 맨 뒤로 간다.
 * 삭제로 생긴 빈 번호는 그대로 둔다 — 순서는 크기 비교에만 쓰여 빈 번호가 있어도 화면 순서는 같다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandService {

    private final MemberRepository memberRepository;
    private final MemberQueryService memberQueryService;
    private final MemberMapper memberMapper;
    private final UploadVerifier uploadVerifier;
    private final Clock clock;

    public AdminMemberResponse create(MemberRequest request) {
        verifyPhotoIfNew(request.photoUrl(), null);

        Member member = Member.builder()
                .status(request.status())
                .sortOrder(memberRepository.findMaxSortOrder(request.status()) + 1)
                .name(request.name())
                .photoUrl(request.photoUrl())
                .department(request.department())
                .history(request.history())
                .interests(request.interests())
                .part(request.part())
                .bio(request.bio())
                .githubUrl(request.links().github())
                .email(request.links().email())
                .consentConfirmedAt(clock.instant())
                .build();

        return memberMapper.toAdminResponse(memberRepository.save(member));
    }

    /** 전체 필드 교체. 구분(코어멤버 ↔ 시니어)이 바뀌면 바뀐 구분의 맨 뒤로 보낸다. */
    public AdminMemberResponse update(Long id, MemberRequest request) {
        Member member = memberQueryService.getOrThrow(id);
        verifyPhotoIfNew(request.photoUrl(), member.getPhotoUrl());

        if (member.getStatus() != request.status()) {
            member.moveTo(memberRepository.findMaxSortOrder(request.status()) + 1);
        }
        member.updateContent(
                request.status(), request.name(), request.photoUrl(), request.department(),
                request.history(), request.interests(), request.part(), request.bio(),
                request.links().github(), request.links().email());
        // 내용이 같은 PUT은 행이 dirty하지 않아 auditing이 updatedAt을 갱신하지 않는다. 저장 요청이면 항상 갱신한다.
        member.touch(clock.instant());

        // updatedAt(@LastModifiedDate)은 flush 시점에 채워지므로, 응답을 만들기 전에 flush해야 새 값이 나간다.
        memberRepository.flush();
        return memberMapper.toAdminResponse(member);
    }

    /**
     * 한 구분의 표시 순서를 통째로 바꾼다. 보낸 id 가 그 구분의 멤버와 정확히 같아야 한다 —
     * 다른 관리자가 그사이 추가 · 삭제했거나 구분을 바꿨으면 400 으로 막고, 화면을 새로 불러 다시 하게 한다.
     * 순서는 내용 수정이 아니라서 수정 시각(updatedAt)은 바꾸지 않는다.
     */
    public void reorder(MemberOrderRequest request) {
        List<Member> members = memberRepository.findAllByStatusOrderBySortOrderAscIdAsc(request.status());
        Set<Long> current = new HashSet<>(members.stream().map(Member::getId).toList());
        if (!current.equals(new HashSet<>(request.ids()))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "ids",
                    "멤버 목록이 바뀌었습니다. 새로 불러온 뒤 다시 시도해 주세요.");
        }
        Map<Long, Member> byId = members.stream().collect(Collectors.toMap(Member::getId, m -> m));
        for (int i = 0; i < request.ids().size(); i++) {
            byId.get(request.ids().get(i)).moveTo(i + 1);
        }
    }

    public void delete(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        memberRepository.deleteById(id);
    }

    /** 값이 새로 들어오거나 바뀐 경우에만 R2를 확인한다(명세 2-9). */
    private void verifyPhotoIfNew(String requested, String current) {
        if (requested != null && !Objects.equals(requested, current)) {
            uploadVerifier.verify(requested, UploadTarget.MEMBER);
        }
    }
}
