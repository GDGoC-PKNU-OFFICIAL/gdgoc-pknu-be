package com.gdgocpknu.gdgoc_pknu_be.common.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** 월 단위 기간 표기 `YYYY.MM` (API 명세서 1-4). DB에는 해당 월의 1일(date)로 저장한다. */
public final class YearMonthText {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy.MM");

    private YearMonthText() {
    }

    public static String format(LocalDate date) {
        return FORMAT.format(date);
    }
}
