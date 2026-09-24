package com.gdgocpknu.gdgoc_pknu_be.project.service;

import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.common.support.YearMonthText;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.Project;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectRepository;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectRequest;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectRequest.TeamMemberRequest;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectResponse;
import com.gdgocpknu.gdgoc_pknu_be.project.mapper.ProjectMapper;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 등록 · 수정 · 삭제(A-12, A-13, 목록 삭제). 슬러그 중복은 저장 전에 먼저 검사하고(도메인 규칙),
 * 동시 저장으로 이 검사를 통과해 버린 경우에는 DB UNIQUE 위반을 GlobalExceptionHandler가
 * 최후 방어선으로 409로 변환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectCommandService {

    private final ProjectRepository projectRepository;
    private final ProjectQueryService projectQueryService;
    private final ProjectMapper projectMapper;

    public ProjectResponse create(ProjectRequest request) {
        requireSlugAvailable(request.slug(), null);

        Project project = Project.builder()
                .slug(request.slug())
                .title(request.title())
                .summary(request.summary())
                .category(request.category())
                .periodStart(YearMonthText.parse(request.period().start()))
                .periodEnd(YearMonthText.parse(request.period().end()))
                .thumbnailUrl(request.thumbnailUrl())
                .githubUrl(request.links().github())
                .demoUrl(request.links().demo())
                .description(request.description())
                .techStack(request.techStack())
                .features(request.features())
                .outcomes(request.outcomes())
                .build();
        applyTeam(project, request.team());

        return projectMapper.toResponse(projectRepository.save(project));
    }

    /** 전체 필드 교체(제약 2). 팀원도 `clear()` 후 다시 채우는 방식으로 통째로 바뀐다. */
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = projectQueryService.getOrThrow(id);
        requireSlugAvailable(request.slug(), id);

        LocalDate periodStart = YearMonthText.parse(request.period().start());
        LocalDate periodEnd = YearMonthText.parse(request.period().end());
        project.updateContent(
                request.slug(), request.title(), request.summary(), request.category(),
                periodStart, periodEnd, request.thumbnailUrl(),
                request.links().github(), request.links().demo(),
                request.description(), request.techStack(), request.features(), request.outcomes());
        project.clearTeam();
        applyTeam(project, request.team());

        return projectMapper.toResponse(project);
    }

    /** 팀원 행(`project_team`)은 `ON DELETE CASCADE`로 함께 삭제된다. */
    public void delete(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        projectRepository.deleteById(id);
    }

    private void requireSlugAvailable(String slug, Long excludeId) {
        if (!projectQueryService.isSlugAvailable(slug, excludeId)) {
            throw new BusinessException(ErrorCode.SLUG_DUPLICATED, "slug", ErrorCode.SLUG_DUPLICATED.message());
        }
    }

    private static void applyTeam(Project project, List<TeamMemberRequest> team) {
        team.forEach(member -> project.addTeamMember(member.name(), member.role()));
    }
}
