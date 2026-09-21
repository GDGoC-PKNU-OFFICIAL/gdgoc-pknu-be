package com.gdgocpknu.gdgoc_pknu_be.common.error;

/** 필드 단위 에러. field는 `period.end`, `history[1]`, `team[0].name`처럼 점 · 인덱스 표기. */
public record FieldErrorItem(String field, String message) {
}
