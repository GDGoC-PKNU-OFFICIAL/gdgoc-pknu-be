package com.gdgocpknu.gdgoc_pknu_be.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 리스트 안에 같은 값(문자열은 앞뒤 공백 제거 후 비교)이 두 번 이상 있으면 실패한다. `null`은 통과(별도 `@NotNull`이 검사). */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoDuplicatesValidator.class)
public @interface NoDuplicates {

    String message() default "중복된 값이 있습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
