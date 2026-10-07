package com.gdgocpknu.gdgoc_pknu_be.auth.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** API 명세서 4-1. */
public record LoginRequest(
        @NotBlank @Size(max = 40) String loginId,
        // 비밀번호는 앞뒤 공백도 값의 일부다 — JacksonConfig의 전역 트리밍에서 이 필드만 뺀다.
        @NotBlank @JsonDeserialize(using = StringDeserializer.class) String password) {
}
