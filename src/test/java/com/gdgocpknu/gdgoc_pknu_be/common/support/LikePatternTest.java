package com.gdgocpknu.gdgoc_pknu_be.common.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LikePatternTest {

    @Test
    void 소문자로_바꾸고_부분_일치_패턴으로_감싼다() {
        assertThat(LikePattern.containsIgnoreCase("Campus")).isEqualTo("%campus%");
    }

    @Test
    void 와일드카드와_이스케이프_문자는_글자로_찾도록_이스케이프한다() {
        assertThat(LikePattern.containsIgnoreCase("100%_a\\b")).isEqualTo("%100\\%\\_a\\\\b%");
    }
}
