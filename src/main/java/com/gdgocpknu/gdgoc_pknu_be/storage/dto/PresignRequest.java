package com.gdgocpknu.gdgoc_pknu_be.storage.dto;

import com.gdgocpknu.gdgoc_pknu_be.storage.UploadTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * API 명세서 4-8 PresignRequest. 허용 형식과 5MB 제한은 전용 에러 코드(`INVALID_FILE_TYPE`, `FILE_TOO_LARGE`)로
 * 응답해야 하므로 PresignService가 검사하고, 여기서는 값의 존재만 본다.
 */
public record PresignRequest(
        @NotNull UploadTarget target,
        @NotBlank String contentType,
        @NotNull @Positive Long size) {
}
