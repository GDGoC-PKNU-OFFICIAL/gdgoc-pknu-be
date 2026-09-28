package com.gdgocpknu.gdgoc_pknu_be.common.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

class SortWhitelistTest {

    private final SortWhitelist whitelist = SortWhitelist.of("periodStart", "title", "updatedAt");

    @Test
    void 필드와_방향을_Sort로_바꾼다() {
        assertThat(whitelist.parse("periodStart,desc")).isEqualTo(Sort.by(Sort.Direction.DESC, "periodStart"));
        assertThat(whitelist.parse("title,asc")).isEqualTo(Sort.by(Sort.Direction.ASC, "title"));
    }

    @Test
    void 방향을_생략하면_오름차순이고_방향은_대소문자를_가리지_않는다() {
        assertThat(whitelist.parse("title")).isEqualTo(Sort.by(Sort.Direction.ASC, "title"));
        assertThat(whitelist.parse("updatedAt,DESC")).isEqualTo(Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"id,desc", "slug", "period_start,desc", "title,up", "title,asc,extra", "", ","})
    void 허용하지_않는_필드나_형식은_sort_필드_에러로_400이다(String value) {
        assertThatThrownBy(() -> whitelist.parse(value))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(e.fieldErrors()).singleElement()
                            .satisfies(item -> assertThat(item.field()).isEqualTo("sort"));
                });
    }
}
