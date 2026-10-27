package com.gdgocpknu.gdgoc_pknu_be.member.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 코어멤버는 역할 필수, 시니어는 역할 금지 (API 명세서 4-6). {@link MemberRequest}에 붙이며,
 * 위반 시 폼의 역할 선택 칸에 에러가 보이도록 `role` 필드에 에러를 단다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RoleMatchesStatusValidator.class)
public @interface RoleMatchesStatus {

    String message() default "구분과 역할이 맞지 않습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
