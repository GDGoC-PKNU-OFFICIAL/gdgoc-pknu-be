package com.gdgocpknu.gdgoc_pknu_be.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoDuplicatesValidator implements ConstraintValidator<NoDuplicates, List<?>> {

    @Override
    public boolean isValid(List<?> value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        Set<Object> seen = new HashSet<>();
        for (Object item : value) {
            Object key = item instanceof String s ? s.trim() : item;
            if (!seen.add(key)) {
                return false;
            }
        }
        return true;
    }
}
