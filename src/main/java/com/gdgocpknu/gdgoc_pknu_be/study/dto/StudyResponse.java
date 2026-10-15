package com.gdgocpknu.gdgoc_pknu_be.study.dto;

import com.gdgocpknu.gdgoc_pknu_be.common.support.PeriodStatus;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * API 명세서 3-2 Study. 공개 · 관리자 응답이 같은 모양이다.
 * 값이 없는 선택 필드(format, period.end)는 전역 설정(non_null)으로 응답에서 생략된다.
 */
public record StudyResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String topic,
        PeriodStatus status,
        Period period,
        String leader,
        List<String> participants,
        String format,
        List<Output> outputs,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    /** start · end는 `YYYY.MM`. end가 없으면 진행 중. */
    public record Period(String start, String end) {
    }

    public record Output(String label, String url) {
    }
}
