package com.gdgocpknu.gdgoc_pknu_be.common.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.gdgocpknu.gdgoc_pknu_be.common.config.ClockConfig;

/** "오늘" · "이번 달" 계산은 모두 이곳을 거친다. 주입된 Clock이 UTC여도 항상 KST 기준으로 계산한다. */
@Component
@RequiredArgsConstructor
public class KstDates {

    private final Clock clock;

    public LocalDate today() {
        return LocalDate.now(clock.withZone(ClockConfig.KST));
    }

    public LocalDate firstDayOfThisMonth() {
        return today().withDayOfMonth(1);
    }

    /** 응답용 일시(API 명세서 1-4): KST 오프셋(+09:00), 초 단위. */
    public OffsetDateTime toKst(Instant instant) {
        return instant.atZone(ClockConfig.KST).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS);
    }
}
