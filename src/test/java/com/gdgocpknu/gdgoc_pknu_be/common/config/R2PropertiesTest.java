package com.gdgocpknu.gdgoc_pknu_be.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** 빠진 `R2_*` 환경변수가 오류 메시지에 이름으로 나오는지 확인한다. Spring 컨텍스트 · DB가 필요 없다. */
class R2PropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 모든_값이_있으면_통과한다() {
        assertThat(validator.validate(new R2Properties("acct", "ak", "sk", "bucket", "https://b.r2.dev"))).isEmpty();
    }

    @Test
    void 빈_값이면_어느_환경변수가_빠졌는지_메시지에_이름이_나온다() {
        Set<ConstraintViolation<R2Properties>> violations = validator.validate(new R2Properties("", " ", null, "", ""));

        assertThat(violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        "환경변수 R2_ACCOUNT_ID를 설정해야 합니다.",
                        "환경변수 R2_ACCESS_KEY를 설정해야 합니다.",
                        "환경변수 R2_SECRET_KEY를 설정해야 합니다.",
                        "환경변수 R2_BUCKET을 설정해야 합니다.",
                        "환경변수 R2_PUBLIC_BASE_URL을 설정해야 합니다.");
    }
}
