package com.gdgocpknu.gdgoc_pknu_be.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code JWT_SECRET}(HS256 서명 키) · {@code JWT_EXPIRATION_HOURS}(기본 8). */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, int expirationHours) {
}
