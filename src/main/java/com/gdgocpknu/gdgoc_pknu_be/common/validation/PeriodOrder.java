package com.gdgocpknu.gdgoc_pknu_be.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * `start ≤ end`를 검사하는 클래스 레벨 제약. {@link HasYearMonthRange}를 구현한 요청 record(기간)에 붙인다.
 * 형식(`YYYY.MM`)은 각 필드의 `@Pattern`이 따로 검사하므로, 여기서는 둘 다 파싱 가능할 때만 순서를 비교한다.
 * 위반 시 프론트 경로 표기(`period.end`)와 맞물리도록 `end` 필드에 에러를 단다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PeriodOrderValidator.class)
public @interface PeriodOrder {

    String message() default "종료월은 시작월보다 빠를 수 없습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
