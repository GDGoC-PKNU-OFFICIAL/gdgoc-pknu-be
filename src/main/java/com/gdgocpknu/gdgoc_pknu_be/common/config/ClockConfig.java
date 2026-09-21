package com.gdgocpknu.gdgoc_pknu_be.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 시간 의존 로직(status 계산 등)은 반드시 이 Clock을 주입받는다. LocalDate.now() 직접 호출 금지. */
@Configuration
public class ClockConfig {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(KST);
    }
}
