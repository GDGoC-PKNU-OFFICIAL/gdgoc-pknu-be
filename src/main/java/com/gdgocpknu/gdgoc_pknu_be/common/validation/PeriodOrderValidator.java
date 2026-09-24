package com.gdgocpknu.gdgoc_pknu_be.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class PeriodOrderValidator implements ConstraintValidator<PeriodOrder, HasYearMonthRange> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy.MM");

    @Override
    public boolean isValid(HasYearMonthRange value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        YearMonth start = parse(value.start());
        YearMonth end = parse(value.end());
        // null이거나 형식이 깨진 값은 @NotBlank·@Pattern이 따로 잡으므로 여기서는 통과시킨다.
        if (start == null || end == null || !end.isBefore(start)) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("end")
                .addConstraintViolation();
        return false;
    }

    private static YearMonth parse(String text) {
        if (text == null) {
            return null;
        }
        try {
            return YearMonth.parse(text, FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
