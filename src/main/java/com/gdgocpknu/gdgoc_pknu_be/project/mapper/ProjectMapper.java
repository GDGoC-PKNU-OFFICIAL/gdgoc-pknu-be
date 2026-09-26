package com.gdgocpknu.gdgoc_pknu_be.project.mapper;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.common.support.PeriodStatus;
import com.gdgocpknu.gdgoc_pknu_be.common.support.YearMonthText;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.Project;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectResponse;

/** Entity → 응답 DTO. 저장하지 않는 계산값(status)과 표기 변환(YYYY.MM, KST 오프셋)을 여기서 채운다. */
@Component
@RequiredArgsConstructor
public class ProjectMapper {

    private final KstDates kstDates;

    public ProjectResponse toResponse(Project project) {
        return toResponse(project, kstDates.firstDayOfThisMonth());
    }

    /** 한 응답 안에서 "이번 달" 기준이 흔들리지 않도록 기준일을 한 번만 계산한다. */
    public List<ProjectResponse> toResponses(List<Project> projects) {
        LocalDate firstDayOfThisMonth = kstDates.firstDayOfThisMonth();
        return projects.stream().map(project -> toResponse(project, firstDayOfThisMonth)).toList();
    }

    private ProjectResponse toResponse(Project project, LocalDate firstDayOfThisMonth) {
        LocalDate periodEnd = project.getPeriodEnd();
        return new ProjectResponse(
                project.getId(),
                project.getSlug(),
                project.getTitle(),
                project.getSummary(),
                List.copyOf(project.getDescription()),
                project.getCategory(),
                PeriodStatus.of(periodEnd, firstDayOfThisMonth),
                new ProjectResponse.Period(
                        YearMonthText.format(project.getPeriodStart()),
                        periodEnd == null ? null : YearMonthText.format(periodEnd)),
                project.getTeam().stream()
                        .map(member -> new ProjectResponse.TeamMember(member.getName(), member.getRole()))
                        .toList(),
                List.copyOf(project.getTechStack()),
                List.copyOf(project.getFeatures()),
                List.copyOf(project.getOutcomes()),
                project.getThumbnailUrl(),
                new ProjectResponse.Links(project.getGithubUrl(), project.getDemoUrl()),
                kstDates.toKst(project.getCreatedAt()),
                kstDates.toKst(project.getUpdatedAt()));
    }
}
