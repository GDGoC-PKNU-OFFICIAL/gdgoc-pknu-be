package com.gdgocpknu.gdgoc_pknu_be.common.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** "달이 바뀌면 관리자 수정 없이 과거 섹션으로 이동한다"를 Clock.fixed로 검증한다. */
class PeriodStatusTest {

    private static final LocalDate SEP_2026 = LocalDate.of(2026, 9, 1);

    @Test
    void 종료월이_없으면_진행_중이라_현재다() {
        assertThat(PeriodStatus.of(null, SEP_2026)).isEqualTo(PeriodStatus.CURRENT);
    }

    @Test
    void 종료월이_이번_달이면_현재다() {
        assertThat(PeriodStatus.of(LocalDate.of(2026, 9, 1), SEP_2026)).isEqualTo(PeriodStatus.CURRENT);
    }

    @Test
    void 종료월이_미래면_현재다() {
        assertThat(PeriodStatus.of(LocalDate.of(2026, 12, 1), SEP_2026)).isEqualTo(PeriodStatus.CURRENT);
    }

    @Test
    void 종료월이_지난달이면_과거다() {
        assertThat(PeriodStatus.of(LocalDate.of(2026, 8, 1), SEP_2026)).isEqualTo(PeriodStatus.PAST);
    }

    @Test
    void 달이_바뀌는_KST_자정에_현재에서_과거로_넘어간다() {
        LocalDate end = LocalDate.of(2026, 9, 1);
        KstDates beforeMidnight = new KstDates(Clock.fixed(Instant.parse("2026-09-30T14:59:59Z"), ZoneOffset.UTC));
        KstDates afterMidnight = new KstDates(Clock.fixed(Instant.parse("2026-09-30T15:00:00Z"), ZoneOffset.UTC));

        assertThat(PeriodStatus.of(end, beforeMidnight.firstDayOfThisMonth())).isEqualTo(PeriodStatus.CURRENT);
        assertThat(PeriodStatus.of(end, afterMidnight.firstDayOfThisMonth())).isEqualTo(PeriodStatus.PAST);
    }
}
