package com.gdgocpknu.gdgoc_pknu_be.project.service;

import com.gdgocpknu.gdgoc_pknu_be.common.dto.PageResponse;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.Project;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectRepository;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectResponse;
import com.gdgocpknu.gdgoc_pknu_be.project.mapper.ProjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryService {

    /** 공개 목록 정렬: period.start 최신순 (같은 달이면 나중에 등록한 것 먼저). */
    private static final Sort PUBLIC_ORDER = Sort.by(Sort.Order.desc("periodStart"), Sort.Order.desc("id"));

    /**
     * 관리자 목록의 임시 기본 정렬. `q`·`status`·`category`·`year`·`sort` 동적 조건과 정렬 화이트리스트는
     * 다음 조각(`ProjectSpecifications`)에서 추가한다 — 지금은 페이징만 지원한다.
     */
    private static final Sort ADMIN_DEFAULT_ORDER = PUBLIC_ORDER;

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    /** 현재 · 과거 프로젝트 전체. 연도별 묶기는 프론트에서 처리한다. */
    public List<ProjectResponse> findAllForPublic() {
        return projectMapper.toResponses(projectRepository.findAll(PUBLIC_ORDER));
    }

    public PageResponse<ProjectResponse> findAllForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, ADMIN_DEFAULT_ORDER);
        return PageResponse.of(projectRepository.findAll(pageable), projectMapper::toResponse);
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
