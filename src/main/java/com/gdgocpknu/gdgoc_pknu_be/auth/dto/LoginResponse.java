package com.gdgocpknu.gdgoc_pknu_be.auth.dto;

import java.time.OffsetDateTime;

/** API 명세서 3-8. 토큰은 Next.js BFF만 받고 브라우저에 직접 내려주지 않는다(PRD 4-3). */
public record LoginResponse(String token, OffsetDateTime expiresAt) {
}
