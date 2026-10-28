package com.gdgocpknu.gdgoc_pknu_be.member.dto;

import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RoleMatchesStatusValidator implements ConstraintValidator<RoleMatchesStatus, MemberRequest> {

    private static final String CORE_NEEDS_ROLE = "코어멤버는 역할을 선택해야 합니다.";
    private static final String SENIOR_NO_ROLE = "시니어는 역할을 지정할 수 없습니다.";

    @Override
    public boolean isValid(MemberRequest request, ConstraintValidatorContext context) {
        // status가 없으면 @NotNull이 따로 잡는다.
        if (request == null || request.status() == null) {
            return true;
        }
        if (request.status() == MemberStatus.CORE && request.role() == null) {
            return fail(context, CORE_NEEDS_ROLE);
        }
        if (request.status() == MemberStatus.SENIOR && request.role() != null) {
            return fail(context, SENIOR_NO_ROLE);
        }
        return true;
    }

    private static boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("role")
                .addConstraintViolation();
        return false;
    }
}
