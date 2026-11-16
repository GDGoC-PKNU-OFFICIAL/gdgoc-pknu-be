package com.gdgocpknu.gdgoc_pknu_be.common.config;

import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cloudflare R2 접속 정보(`R2_*` 환경변수). 값이 비어 있으면 기동이 실패한다. application.yml이 빈 값을 기본으로 두고
 * 여기서 막는 이유는, 기본값 없는 `${R2_ACCOUNT_ID}`는 풀리지 않은 채 문자열 그대로 들어가 `Illegal character in
 * authority ...` 같은 엉뚱한 오류로 터지기 때문이다. 이렇게 하면 어느 환경변수가 빠졌는지 오류 메시지에 나온다.
 */
@Validated
@ConfigurationProperties(prefix = "app.r2")
public record R2Properties(
        @NotBlank(message = "환경변수 R2_ACCOUNT_ID를 설정해야 합니다.") String accountId,
        @NotBlank(message = "환경변수 R2_ACCESS_KEY를 설정해야 합니다.") String accessKey,
        @NotBlank(message = "환경변수 R2_SECRET_KEY를 설정해야 합니다.") String secretKey,
        @NotBlank(message = "환경변수 R2_BUCKET을 설정해야 합니다.") String bucket,
        @NotBlank(message = "환경변수 R2_PUBLIC_BASE_URL을 설정해야 합니다.") String publicBaseUrl) {

    public URI endpoint() {
        return URI.create("https://" + accountId + ".r2.cloudflarestorage.com");
    }

    /** 공개 URL 조립과 재검증의 접두사 비교가 같은 값을 보도록 끝의 `/`를 제거한다. */
    public String normalizedPublicBaseUrl() {
        return publicBaseUrl.replaceAll("/+$", "");
    }
}
