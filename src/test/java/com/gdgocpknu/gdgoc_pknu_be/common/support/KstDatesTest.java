package com.gdgocpknu.gdgoc_pknu_be.common.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class KstDatesTest {

    @Test
    void UTC로는_전날이어도_KST_기준_날짜를_반환한다() {
        // 2026-09-30T15:00:00Z == 2026-10-01T00:00:00+09:00
        KstDates kstDates = new KstDates(Clock.fixed(Instant.parse("2026-09-30T15:00:00Z"), ZoneOffset.UTC));

        assertThat(kstDates.today()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(kstDates.firstDayOfThisMonth()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void KST_자정_1초_전은_아직_전날이다() {
        KstDates kstDates = new KstDates(Clock.fixed(Instant.parse("2026-09-30T14:59:59Z"), ZoneOffset.UTC));

        assertThat(kstDates.today()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(kstDates.firstDayOfThisMonth()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void 응답_일시는_KST_오프셋과_초_단위로_변환된다() {
        KstDates kstDates = new KstDates(Clock.systemUTC());

        OffsetDateTime result = kstDates.toKst(Instant.parse("2026-09-15T10:30:00.123456Z"));

        assertThat(result).isEqualTo(OffsetDateTime.parse("2026-09-15T19:30:00+09:00"));
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.ofHours(9));
    }
}
