package com.gdgocpknu.gdgoc_pknu_be.setting.dto;

import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingRequest.RecruitPeriodRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidRecruitPeriodValidator implements ConstraintValidator<ValidRecruitPeriod, RecruitPeriodRequest> {

    @Override
    public boolean isValid(RecruitPeriodRequest period, ConstraintValidatorContext context) {
        if (period == null || (period.start() == null && period.end() == null)) {
            return true;
        }
        if (period.start() == null) {
            return fail(context, "start", "모집 시작일을 입력해야 합니다.");
        }
        if (period.end() == null) {
            return fail(context, "end", "모집 종료일을 입력해야 합니다.");
        }
        if (period.end().isBefore(period.start())) {
            return fail(context, "end", "종료일은 시작일보다 빠를 수 없습니다.");
        }
        return true;
    }

    private static boolean fail(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
