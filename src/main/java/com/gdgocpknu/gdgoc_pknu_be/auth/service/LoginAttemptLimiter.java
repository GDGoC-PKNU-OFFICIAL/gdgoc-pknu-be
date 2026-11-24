package com.gdgocpknu.gdgoc_pknu_be.auth.service;

import com.gdgocpknu.gdgoc_pknu_be.auth.LoginThrottleProperties;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 로그인 시도 제한. 비밀번호 대입을 늦추고, 비로그인으로 부르는 로그인이 BCrypt(CPU)를 무한정 쓰지 못하게 한다.
 *
 * <p>시도를 **결과가 나오기 전에** 센다({@link #acquire()}). 실패한 뒤에 세면 동시에 몰린 요청이 모두 검사를
 * 통과해 BCrypt가 한꺼번에 돌기 때문이다. 성공하면 횟수를 비운다({@link #recordSuccess()}).
 * 횟수를 넘기면 잠그고, 잠긴 동안에는 BCrypt는커녕 DB 조회도 하지 않고 바로 거절한다.
 *
 * <p>상태는 이 인스턴스의 메모리에만 있다(재기동하면 풀린다). 배포가 인스턴스 하나이므로 충분하다.
 * 잠금은 공격자가 관리자 로그인을 잠시 막을 수 있다는 뜻이기도 하다 — 잠금 시간을 짧게 둔 이유다.
 */
@Slf4j
@Component
public class LoginAttemptLimiter {

    private final LoginThrottleProperties properties;
    private final Clock clock;

    private final Deque<Instant> attempts = new ArrayDeque<>();
    private Instant lockedUntil;

    public LoginAttemptLimiter(LoginThrottleProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** 시도 하나를 시작한다. 잠겨 있거나 이 시도로 한도를 넘으면 {@link ErrorCode#LOGIN_LOCKED}다. */
    public synchronized void acquire() {
        Instant now = clock.instant();
        if (lockedUntil != null) {
            if (now.isBefore(lockedUntil)) {
                throw locked(Duration.between(now, lockedUntil));
            }
            lockedUntil = null;
        }

        Instant windowStart = now.minus(properties.window());
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(windowStart)) {
            attempts.pollFirst();
        }
        if (attempts.size() >= properties.maxAttempts()) {
            lockedUntil = now.plus(properties.lockDuration());
            attempts.clear();
            log.warn("로그인 시도가 {}회를 넘어 {}분 동안 잠급니다.", properties.maxAttempts(),
                    properties.lockDuration().toMinutes());
            throw locked(properties.lockDuration());
        }
        attempts.addLast(now);
    }

    /** 로그인에 성공하면 그동안의 시도를 지운다. */
    public synchronized void recordSuccess() {
        attempts.clear();
    }

    /** 테스트에서 상태를 비운다. 운영 코드는 부르지 않는다. */
    public synchronized void reset() {
        attempts.clear();
        lockedUntil = null;
    }

    private static BusinessException locked(Duration retryAfter) {
        return new BusinessException(ErrorCode.LOGIN_LOCKED, retryAfter);
    }
}
