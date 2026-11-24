package com.gdgocpknu.gdgoc_pknu_be.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 로그인 시도 제한(`app.login-throttle.*`). 관리자 계정이 하나뿐이고 브라우저가 아니라 Next 서버(BFF)가 호출하므로
 * 요청 IP가 아니라 로그인 시도 전체를 하나로 센다.
 *
 * @param maxAttempts  성공하지 못한 시도를 `window` 안에 이만큼까지 받는다. 넘으면 `lockDuration` 동안 모두 거절한다.
 * @param window       시도를 세는 기간
 * @param lockDuration 잠금 시간. 이 시간이 지나면 시도 횟수를 새로 센다.
 */
@Validated
@ConfigurationProperties(prefix = "app.login-throttle")
public record LoginThrottleProperties(
        @Positive int maxAttempts,
        @NotNull Duration window,
        @NotNull Duration lockDuration) {
}
