package com.gdgocpknu.gdgoc_pknu_be.common.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** 관리자 목록 공통 응답 `Page<T>` (API 명세서 3-7). page는 0부터 시작. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<? super E, ? extends T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().<T>map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
