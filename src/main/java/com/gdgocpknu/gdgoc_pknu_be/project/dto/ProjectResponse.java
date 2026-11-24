package com.gdgocpknu.gdgoc_pknu_be.project.dto;

import java.time.OffsetDateTime;
import java.util.List;
import com.gdgocpknu.gdgoc_pknu_be.common.support.PeriodStatus;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.BadgeColor;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectCategory;

/**
 * API 명세서 3-1 Project. 공개 · 관리자 응답이 같은 모양이다.
 * 값이 없는 선택 필드(badge, thumbnailUrl, period.end, links.*)는 전역 설정(non_null)으로 응답에서 생략된다.
 */
public record ProjectResponse(
        Long id,
        String slug,
        String title,
        String summary,
        List<String> description,
        ProjectCategory category,
        Badge badge,
        PeriodStatus status,
        Period period,
        List<TeamMember> team,
        List<String> techStack,
        List<String> features,
        List<String> outcomes,
        String thumbnailUrl,
        Links links,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    /** start · end는 `YYYY.MM`. end가 없으면 진행 중. */
    public record Period(String start, String end) {
    }

    /** 직접 정한 카드 배지. 없으면 응답에서 생략되고, 화면은 category 라벨 · 색을 쓴다. */
    public record Badge(String label, BadgeColor color) {
    }

    public record TeamMember(String name, String role) {
    }

    public record Links(String github, String demo) {
    }
}
