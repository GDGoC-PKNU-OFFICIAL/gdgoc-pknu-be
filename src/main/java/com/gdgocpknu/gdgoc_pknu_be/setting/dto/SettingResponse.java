package com.gdgocpknu.gdgoc_pknu_be.setting.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * API 명세서 3-5 SiteSetting. 공개(`GET /api/settings`)와 관리자(`GET /api/admin/settings`) 응답이 같다.
 * 모집 기간이 없으면 `recruitPeriod`가, 히어로 사진을 바꾸지 않았으면 `heroImageUrl` · `heroImagePosition`이 생략된다.
 */
public record SettingResponse(
        boolean recruiting,
        RecruitPeriod recruitPeriod,
        List<Stat> stats,
        String heroImageUrl,
        String heroImagePosition,
        OffsetDateTime updatedAt) {

    public record RecruitPeriod(LocalDate start, LocalDate end) {
    }

    public record Stat(String label, int value) {
    }
}
