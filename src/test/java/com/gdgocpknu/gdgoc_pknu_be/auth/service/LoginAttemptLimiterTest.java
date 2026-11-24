package com.gdgocpknu.gdgoc_pknu_be.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gdgocpknu.gdgoc_pknu_be.auth.LoginThrottleProperties;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** 시간은 직접 움직이는 시계로 고정해, 잠금 · 윈도우 만료를 실제로 기다리지 않고 확인한다. */
class LoginAttemptLimiterTest {

    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final Duration LOCK = Duration.ofMinutes(5);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-23T00:00:00Z"));
    private final LoginAttemptLimiter limiter =
            new LoginAttemptLimiter(new LoginThrottleProperties(5, WINDOW, LOCK), clock);

    @Test
    void 한도까지는_받고_하나_넘으면_잠그고_남은_시간을_알려_준다() {
        acquire(5);

        assertThatThrownBy(limiter::acquire)
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo(ErrorCode.LOGIN_LOCKED);
                    assertThat(e.retryAfter()).isEqualTo(LOCK);
                });
    }

    @Test
    void 잠긴_동안에는_계속_거절하고_남은_시간이_줄어든다() {
        acquire(5);
        assertThatThrownBy(limiter::acquire).isInstanceOf(BusinessException.class);

        clock.advance(Duration.ofMinutes(2));

        assertThatThrownBy(limiter::acquire)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.retryAfter()).isEqualTo(Duration.ofMinutes(3)));
    }

    @Test
    void 잠금이_끝나면_횟수를_새로_센다() {
        acquire(5);
        assertThatThrownBy(limiter::acquire).isInstanceOf(BusinessException.class);

        clock.advance(LOCK);

        acquire(5);   // 잠금이 풀리고 윈도우 안에 남아 있던 이전 시도도 지워져 한도를 다시 받는다
        assertThatThrownBy(limiter::acquire).isInstanceOf(BusinessException.class);
    }

    @Test
    void 성공하면_그동안의_시도를_지운다() {
        acquire(4);
        limiter.recordSuccess();

        acquire(5);
    }

    @Test
    void 윈도우보다_오래된_시도는_세지_않는다() {
        acquire(4);
        clock.advance(WINDOW.plusSeconds(1));

        acquire(5);
    }

    @Test
    void 동시에_몰려도_한도만큼만_통과한다() throws Exception {
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    limiter.acquire();
                    allowed.incrementAndGet();
                } catch (BusinessException e) {
                    rejected.incrementAndGet();
                }
                return null;
            });
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        assertThat(allowed.get()).as("결과를 기다리지 않고 시도를 세므로 동시 요청도 한도를 넘지 못한다").isEqualTo(5);
        assertThat(rejected.get()).isEqualTo(threads - 5);
    }

    @Test
    void reset하면_잠금과_횟수가_모두_사라진다() {
        acquire(5);
        assertThatThrownBy(limiter::acquire).isInstanceOf(BusinessException.class);

        limiter.reset();

        assertThatCode(() -> acquire(5)).doesNotThrowAnyException();
    }

    private void acquire(int times) {
        for (int i = 0; i < times; i++) {
            limiter.acquire();
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
