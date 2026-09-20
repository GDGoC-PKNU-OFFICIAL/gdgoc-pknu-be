package com.gdgocpknu.gdgoc_pknu_be.project.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectRepository;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectResponse;
import com.gdgocpknu.gdgoc_pknu_be.project.mapper.ProjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryService {

    /** 공개 목록 정렬: period.start 최신순 (같은 달이면 나중에 등록한 것 먼저). */
    private static final Sort PUBLIC_ORDER = Sort.by(Sort.Order.desc("periodStart"), Sort.Order.desc("id"));

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    /** 현재 · 과거 프로젝트 전체. 연도별 묶기는 프론트에서 처리한다. */
    public List<ProjectResponse> findAllForPublic() {
        return projectMapper.toResponses(projectRepository.findAll(PUBLIC_ORDER));
    }
}
