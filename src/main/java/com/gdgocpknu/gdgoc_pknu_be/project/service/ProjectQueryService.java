package com.gdgocpknu.gdgoc_pknu_be.project.service;

import com.gdgocpknu.gdgoc_pknu_be.common.dto.PageResponse;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.common.support.SortWhitelist;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.Project;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectRepository;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectSpecifications;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectResponse;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectSearchCond;
import com.gdgocpknu.gdgoc_pknu_be.project.mapper.ProjectMapper;
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
public class ProjectQueryService {

    /** 공개 목록 정렬: period.start 최신순 (같은 달이면 나중에 등록한 것 먼저). */
    private static final Sort PUBLIC_ORDER = Sort.by(Sort.Order.desc("periodStart"), Sort.Order.desc("id"));

    /** 관리자 목록 `sort` 허용 필드 (API 명세서 2-4). 기본값 `periodStart,desc`는 컨트롤러가 채운다. */
    private static final SortWhitelist ADMIN_SORTABLE = SortWhitelist.of("periodStart", "title", "updatedAt");

    /** 정렬 값이 같은 행이 페이지 경계에서 중복 · 누락되지 않도록 마지막에 붙이는 기준. */
    private static final Sort TIE_BREAKER = Sort.by(Sort.Order.desc("id"));

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final KstDates kstDates;

    /** 현재 · 과거 프로젝트 전체. 연도별 묶기는 프론트에서 처리한다. */
    public List<ProjectResponse> findAllForPublic() {
        return projectMapper.toResponses(projectRepository.findAll(PUBLIC_ORDER));
    }

    /**
     * 관리자 목록(A-11). `status` 필터와 응답의 `status`가 같은 "이번 달"을 보도록 기준일을 한 번만 계산한다 —
     * 월말 자정에 걸친 요청에서 `status=past`로 걸렀는데 `current`가 섞여 나오는 일을 막는다.
     */
    public PageResponse<ProjectResponse> findAllForAdmin(ProjectSearchCond cond, int page, int size, String sort) {
        LocalDate firstDayOfThisMonth = kstDates.firstDayOfThisMonth();
        Pageable pageable = PageRequest.of(page, size, ADMIN_SORTABLE.parse(sort).and(TIE_BREAKER));
        Page<Project> result = projectRepository.findAll(adminFilter(cond, firstDayOfThisMonth), pageable);
        return PageResponse.of(result, project -> projectMapper.toResponse(project, firstDayOfThisMonth));
    }

    private static Specification<Project> adminFilter(ProjectSearchCond cond, LocalDate firstDayOfThisMonth) {
        List<Specification<Project>> specs = new ArrayList<>();
        if (cond.q() != null) {
            specs.add(ProjectSpecifications.titleContains(cond.q()));
        }
        if (cond.status() != null) {
            specs.add(ProjectSpecifications.status(cond.status(), firstDayOfThisMonth));
        }
        if (cond.category() != null) {
            specs.add(ProjectSpecifications.category(cond.category()));
        }
        if (cond.year() != null) {
            specs.add(ProjectSpecifications.startYear(cond.year()));
        }
        return Specification.allOf(specs);
    }

    public ProjectResponse getForAdmin(Long id) {
        return projectMapper.toResponse(getOrThrow(id));
    }

    public boolean isSlugAvailable(String slug, Long excludeId) {
        return excludeId == null
                ? !projectRepository.existsBySlug(slug)
                : !projectRepository.existsBySlugAndIdNot(slug, excludeId);
    }

    Project getOrThrow(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
