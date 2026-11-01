package com.gdgocpknu.gdgoc_pknu_be.setting.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 모집 기간은 둘 다 입력하거나 둘 다 비워야 하고, 둘 다 있으면 `start ≤ end`다 (API 명세서 4-7).
 * 위반 시 폼에서 잘못된 칸에 에러가 보이도록 `start` · `end` 필드에 에러를 단다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidRecruitPeriodValidator.class)
public @interface ValidRecruitPeriod {

    String message() default "모집 기간이 올바르지 않습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
