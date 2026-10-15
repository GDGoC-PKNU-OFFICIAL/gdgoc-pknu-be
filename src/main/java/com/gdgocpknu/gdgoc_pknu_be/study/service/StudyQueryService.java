package com.gdgocpknu.gdgoc_pknu_be.study.service;

import com.gdgocpknu.gdgoc_pknu_be.common.dto.PageResponse;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.common.support.SortWhitelist;
import com.gdgocpknu.gdgoc_pknu_be.study.domain.Study;
import com.gdgocpknu.gdgoc_pknu_be.study.domain.StudyRepository;
import com.gdgocpknu.gdgoc_pknu_be.study.domain.StudySpecifications;
import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudyResponse;
import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudySearchCond;
import com.gdgocpknu.gdgoc_pknu_be.study.mapper.StudyMapper;
import java.time.LocalDate;
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
public class StudyQueryService {

    /** 공개 목록 정렬: period.start 최신순 (같은 달이면 나중에 등록한 것 먼저). */
    private static final Sort PUBLIC_ORDER = Sort.by(Sort.Order.desc("periodStart"), Sort.Order.desc("id"));

    /** 관리자 목록 `sort` 허용 필드 (API 명세서 2-5). 기본값 `periodStart,desc`는 컨트롤러가 채운다. */
    private static final SortWhitelist ADMIN_SORTABLE = SortWhitelist.of("periodStart", "title", "updatedAt");

    /** 정렬 값이 같은 행이 페이지 경계에서 중복 · 누락되지 않도록 마지막에 붙이는 기준. */
    private static final Sort TIE_BREAKER = Sort.by(Sort.Order.desc("id"));

    private final StudyRepository studyRepository;
    private final StudyMapper studyMapper;
    private final KstDates kstDates;

    /** 현재 · 과거 스터디 전체. 연도별 묶기는 프론트에서 처리한다. */
    public List<StudyResponse> findAllForPublic() {
        return studyMapper.toResponses(studyRepository.findAll(PUBLIC_ORDER));
    }

    /** 관리자 목록(A-21). `status` 필터와 응답 `status`가 같은 "이번 달"을 보도록 기준일을 한 번만 계산한다. */
    public PageResponse<StudyResponse> findAllForAdmin(StudySearchCond cond, int page, int size, String sort) {
        LocalDate firstDayOfThisMonth = kstDates.firstDayOfThisMonth();
        Pageable pageable = PageRequest.of(page, size, ADMIN_SORTABLE.parse(sort).and(TIE_BREAKER));
        Page<Study> result = studyRepository.findAll(adminFilter(cond, firstDayOfThisMonth), pageable);
        return PageResponse.of(result, study -> studyMapper.toResponse(study, firstDayOfThisMonth));
    }

    private static Specification<Study> adminFilter(StudySearchCond cond, LocalDate firstDayOfThisMonth) {
        List<Specification<Study>> specs = new ArrayList<>();
        if (cond.q() != null) {
            specs.add(StudySpecifications.titleOrLeaderContains(cond.q()));
        }
        if (cond.status() != null) {
            specs.add(StudySpecifications.status(cond.status(), firstDayOfThisMonth));
        }
        if (cond.year() != null) {
            specs.add(StudySpecifications.startYear(cond.year()));
        }
        return Specification.allOf(specs);
    }

    public StudyResponse getForAdmin(Long id) {
        return studyMapper.toResponse(getOrThrow(id));
    }

    public boolean isSlugAvailable(String slug, Long excludeId) {
        return excludeId == null
                ? !studyRepository.existsBySlug(slug)
                : !studyRepository.existsBySlugAndIdNot(slug, excludeId);
    }

    Study getOrThrow(Long id) {
        return studyRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
