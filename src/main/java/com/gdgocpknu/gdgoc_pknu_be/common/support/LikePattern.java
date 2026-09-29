package com.gdgocpknu.gdgoc_pknu_be.common.support;

import java.util.Locale;

/**
 * 관리자 목록 검색어(`q`)를 LIKE 패턴으로 바꾼다. 검색어의 `%` · `_`는 와일드카드가 아니라 글자로 찾아야 하므로
 * {@link #ESCAPE}로 이스케이프한다 — 쿼리에서도 같은 이스케이프 문자를 지정해야 한다.
 */
public final class LikePattern {

    public static final char ESCAPE = '\\';

    private LikePattern() {
    }

    /** 대소문자 구분 없는 부분 일치용: 소문자로 바꾸고 `%…%`로 감싼다. 컬럼 쪽도 `lower()`로 비교한다. */
    public static String containsIgnoreCase(String keyword) {
        String escaped = keyword.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
