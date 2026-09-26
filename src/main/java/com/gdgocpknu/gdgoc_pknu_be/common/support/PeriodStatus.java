package com.gdgocpknu.gdgoc_pknu_be.common.support;

import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;

/**
 * 프로젝트 · 스터디의 현재/과거 구분. 저장하지 않고 조회 시 계산한다 (API 명세서 1-4).
 * 목록 필터의 SQL 조건(`period_end IS NULL OR period_end >= 이번 달 1일`)과 같은 규칙이어야 한다.
 */
public enum PeriodStatus {

    CURRENT("current"),
    PAST("past");

    private final String code;

    PeriodStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    /** 종료월이 없거나 이번 달 이후(이번 달 포함)면 현재, 이번 달보다 이전이면 과거. */
    public static PeriodStatus of(LocalDate periodEnd, LocalDate firstDayOfThisMonth) {
        if (periodEnd == null || !periodEnd.isBefore(firstDayOfThisMonth)) {
            return CURRENT;
        }
        return PAST;
    }
}
