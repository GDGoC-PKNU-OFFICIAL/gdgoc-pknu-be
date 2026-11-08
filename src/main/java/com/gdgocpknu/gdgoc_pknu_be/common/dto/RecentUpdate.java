package com.gdgocpknu.gdgoc_pknu_be.common.dto;

import java.time.Instant;

/**
 * 도메인별 최근 수정 항목(대시보드 조합용). `updatedAt`을 `Instant`로 두는 이유는, 응답 형식(초 단위)으로 자른 값으로
 * 합쳐 정렬하면 같은 초에 수정된 항목의 순서가 틀어지기 때문이다.
 */
public record RecentUpdate(String resource, Long id, String title, Instant updatedAt) {
}
