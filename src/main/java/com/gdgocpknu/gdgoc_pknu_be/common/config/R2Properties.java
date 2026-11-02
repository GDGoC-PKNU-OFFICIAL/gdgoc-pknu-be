package com.gdgocpknu.gdgoc_pknu_be.common.config;

import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Cloudflare R2 접속 정보(`R2_*` 환경변수). 값이 비어 있으면 기동이 실패한다. */
@Validated
@ConfigurationProperties(prefix = "app.r2")
public record R2Properties(
        @NotBlank String accountId,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String bucket,
        @NotBlank String publicBaseUrl) {

    public URI endpoint() {
        return URI.create("https://" + accountId + ".r2.cloudflarestorage.com");
    }

    /** 공개 URL 조립과 재검증의 접두사 비교가 같은 값을 보도록 끝의 `/`를 제거한다. */
    public String normalizedPublicBaseUrl() {
        return publicBaseUrl.replaceAll("/+$", "");
    }
}
