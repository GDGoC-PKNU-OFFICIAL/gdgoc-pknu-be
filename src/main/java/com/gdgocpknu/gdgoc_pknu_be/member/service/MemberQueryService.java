package com.gdgocpknu.gdgoc_pknu_be.member.service;

import com.gdgocpknu.gdgoc_pknu_be.common.dto.PageResponse;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.common.support.SortWhitelist;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.Member;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRepository;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberSpecifications;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.AdminMemberResponse;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberResponse;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberSearchCond;
import com.gdgocpknu.gdgoc_pknu_be.member.mapper.MemberMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    /** 관리자 목록 `sort` 허용 필드 (API 명세서 2-7). `sort`를 보내지 않으면 기본 정렬(구분 → 역할 → 이름)이다. */
    private static final SortWhitelist ADMIN_SORTABLE = SortWhitelist.of("name", "updatedAt");

    /** 정렬 값이 같은 행이 페이지 경계에서 중복 · 누락되지 않도록 마지막에 붙이는 기준. */
    private static final Sort TIE_BREAKER = Sort.by(Sort.Order.desc("id"));

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    /**
     * 코어멤버 · 시니어 전체. 구분별 묶기 · 정렬은 프론트가 처리하지만(명세 2-1),
     * 서버도 관리자 기본 정렬과 같은 순서로 내려 응답이 항상 같은 순서가 되게 한다.
     */
    public List<MemberResponse> findAllForPublic() {
        return memberMapper.toPublicResponses(memberRepository.findAll(MemberSpecifications.defaultOrder()));
    }

    /**
     * 관리자 목록(A-41). `sort`가 없으면 기본 정렬(구분 → 역할 순 → 이름), 있으면 `name` · `updatedAt` 중 하나다.
     * 기본 정렬은 Specification이 쿼리에 직접 걸므로 이때 Pageable에는 Sort를 주지 않는다.
     */
    public PageResponse<AdminMemberResponse> findAllForAdmin(MemberSearchCond cond, int page, int size, String sort) {
        boolean defaultOrder = sort == null || sort.isBlank();
        Pageable pageable = defaultOrder
                ? PageRequest.of(page, size)
                : PageRequest.of(page, size, ADMIN_SORTABLE.parse(sort).and(TIE_BREAKER));
        Page<Member> result = memberRepository.findAll(adminFilter(cond, defaultOrder), pageable);
        return PageResponse.of(result, memberMapper::toAdminResponse);
    }

    private static Specification<Member> adminFilter(MemberSearchCond cond, boolean defaultOrder) {
        List<Specification<Member>> specs = new ArrayList<>();
        if (cond.q() != null) {
            specs.add(MemberSpecifications.nameContains(cond.q()));
        }
        if (cond.status() != null) {
            specs.add(MemberSpecifications.status(cond.status()));
        }
        if (cond.role() != null) {
            specs.add(MemberSpecifications.role(cond.role()));
        }
        if (defaultOrder) {
            specs.add(MemberSpecifications.defaultOrder());
        }
        return Specification.allOf(specs);
    }

    public AdminMemberResponse getForAdmin(Long id) {
        return memberMapper.toAdminResponse(getOrThrow(id));
    }

    Member getOrThrow(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
