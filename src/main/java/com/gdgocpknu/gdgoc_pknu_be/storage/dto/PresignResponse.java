package com.gdgocpknu.gdgoc_pknu_be.storage.dto;

import java.time.OffsetDateTime;

/** API 명세서 3-8 PresignResponse. */
public record PresignResponse(String uploadUrl, String publicUrl, String key, OffsetDateTime expiresAt) {
}
