package com.gdgocpknu.gdgoc_pknu_be.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Clock을 발급 · 검증에서 각각 고정해, 8시간을 실제로 기다리지 않고 만료를 검증한다. */
class JwtProviderTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-09-30T00:00:00Z");
    private static final Duration EIGHT_HOURS = Duration.ofHours(8);

    @Test
    void 발급한_토큰에서_subject와_만료시각을_그대로_복원한다() {
        JwtProvider provider = providerAt(ISSUED_AT);

        JwtProvider.IssuedToken issued = provider.issue("admin");

        assertThat(issued.expiresAt()).isEqualTo(ISSUED_AT.plus(EIGHT_HOURS));
        assertThat(provider.parseSubject(issued.token())).isEqualTo("admin");
    }

    @Test
    void 만료_1초_전에는_아직_통과한다() {
        String token = providerAt(ISSUED_AT).issue("admin").token();
        JwtProvider verifier = providerAt(ISSUED_AT.plus(EIGHT_HOURS).minusSeconds(1));

        assertThat(verifier.parseSubject(token)).isEqualTo("admin");
    }

    @Test
    void 만료_시각이_지나면_ExpiredJwtException이다() {
        String token = providerAt(ISSUED_AT).issue("admin").token();
        JwtProvider verifier = providerAt(ISSUED_AT.plus(EIGHT_HOURS).plusSeconds(1));

        assertThatThrownBy(() -> verifier.parseSubject(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void 서명이_위조되면_JwtException이다() {
        String token = providerAt(ISSUED_AT).issue("admin").token();
        int lastDot = token.lastIndexOf('.');
        String tampered = token.substring(0, lastDot + 1) + "A".repeat(token.length() - lastDot - 1);

        assertThatThrownBy(() -> providerAt(ISSUED_AT).parseSubject(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void 다른_비밀키로_서명된_토큰은_거부한다() {
        JwtProperties otherSecret = new JwtProperties("another-secret-key-also-at-least-32-bytes-long", 8);
        String token = new JwtProvider(otherSecret, Clock.fixed(ISSUED_AT, ZoneOffset.UTC)).issue("admin").token();

        assertThatThrownBy(() -> providerAt(ISSUED_AT).parseSubject(token)).isInstanceOf(JwtException.class);
    }

    private static JwtProvider providerAt(Instant now) {
        JwtProperties properties = new JwtProperties("test-secret-key-at-least-32-bytes-long!!", 8);
        return new JwtProvider(properties, Clock.fixed(now, ZoneOffset.UTC));
    }
}
