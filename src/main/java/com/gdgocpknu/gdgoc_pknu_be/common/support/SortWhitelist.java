package com.gdgocpknu.gdgoc_pknu_be.common.support;

import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import java.util.List;
import org.springframework.data.domain.Sort;

/**
 * 관리자 목록 `sort` 파라미터(`필드,방향`)를 허용 필드만 통과시켜 `Sort`로 바꾼다 (API 명세서 1-6).
 * 임의의 엔티티 속성으로 정렬하게 두면 인덱스 없는 정렬 · 존재하지 않는 속성(500)이 그대로 열린다.
 * 허용 필드 이름은 API 필드 이름이자 엔티티 속성 이름이다(예: `periodStart`).
 */
public final class SortWhitelist {

    private final List<String> fields;

    private SortWhitelist(List<String> fields) {
        this.fields = List.copyOf(fields);
    }

    public static SortWhitelist of(String... fields) {
        return new SortWhitelist(List.of(fields));
    }

    /** `title` · `title,asc` · `updatedAt,desc`. 방향을 생략하면 오름차순, 방향은 대소문자를 가리지 않는다. */
    public Sort parse(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length > 2) {
            throw invalid();
        }
        String field = parts[0].strip();
        if (!fields.contains(field)) {
            throw invalid();
        }
        Sort.Direction direction = parts.length == 1
                ? Sort.Direction.ASC
                : Sort.Direction.fromOptionalString(parts[1].strip()).orElseThrow(this::invalid);
        return Sort.by(direction, field);
    }

    private BusinessException invalid() {
        return new BusinessException(ErrorCode.VALIDATION_FAILED, "sort",
                "정렬 기준은 '필드,방향' 형식이며, 필드는 " + String.join(" · ", fields) + " 중 하나여야 합니다.");
    }
}
