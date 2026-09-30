package com.gdgocpknu.gdgoc_pknu_be.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * HS256 JWT 발급·검증 (8시간 만료, 리프레시 토큰 없음 — PRD 4-3).
 * 시간은 전부 주입된 {@link Clock}을 거친다 — 테스트에서 발급·검증 시각을 각각 고정해
 * "8시간 뒤 만료됨"을 실제로 8시간 기다리지 않고 검증하기 위해서다.
 */
@Component
public class JwtProvider {

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    public JwtProvider(JwtProperties properties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofHours(properties.expirationHours());
        this.clock = clock;
    }

    public IssuedToken issue(String subject) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(expiration);
        String token = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /**
     * 서명·만료를 검증하고 subject(loginId)를 돌려준다. 위조·형식 오류는 {@link io.jsonwebtoken.JwtException},
     * 만료는 그 하위 타입인 {@link io.jsonwebtoken.ExpiredJwtException}을 던진다 — 호출부(필터)가 구분해서 잡는다.
     */
    public String parseSubject(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
