package com.gdgocpknu.gdgoc_pknu_be.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * `date ≤ endDate`를 검사하는 클래스 레벨 제약. {@link HasDateRange}를 구현한 요청 record에 붙인다.
 * 날짜 형식은 Jackson이 `LocalDate`로 읽으며 이미 검사하므로, 여기서는 둘 다 있을 때만 순서를 비교한다.
 * 위반 시 프론트 경로 표기와 맞물리도록 `endDate` 필드에 에러를 단다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateOrderValidator.class)
public @interface DateOrder {

    String message() default "종료일은 시작일보다 빠를 수 없습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
