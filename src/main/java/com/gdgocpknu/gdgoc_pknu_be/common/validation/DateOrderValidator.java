package com.gdgocpknu.gdgoc_pknu_be.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateOrderValidator implements ConstraintValidator<DateOrder, HasDateRange> {

    @Override
    public boolean isValid(HasDateRange value, ConstraintValidatorContext context) {
        // null 값은 @NotNull이 따로 잡고, 종료일이 없으면 하루짜리 일정이므로 통과시킨다.
        if (value == null || value.date() == null || value.endDate() == null
                || !value.endDate().isBefore(value.date())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("endDate")
                .addConstraintViolation();
        return false;
    }
}
