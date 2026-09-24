package com.gdgocpknu.gdgoc_pknu_be.common.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class YearMonthTextTest {

    @Test
    void 월의_1일을_YYYY_MM으로_표기한다() {
        assertThat(YearMonthText.format(LocalDate.of(2026, 3, 1))).isEqualTo("2026.03");
        assertThat(YearMonthText.format(LocalDate.of(2025, 12, 1))).isEqualTo("2025.12");
    }

    @Test
    void YYYY_MM을_해당_월의_1일로_되돌린다() {
        assertThat(YearMonthText.parse("2026.03")).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(YearMonthText.parse(null)).isNull();
    }
}
