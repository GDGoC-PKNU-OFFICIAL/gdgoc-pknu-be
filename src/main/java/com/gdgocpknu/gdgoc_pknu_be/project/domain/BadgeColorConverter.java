package com.gdgocpknu.gdgoc_pknu_be.project.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** 배지 색을 DB CHECK(코드값 목록)와 맞도록 코드값으로 저장한다 ({@link ProjectCategoryConverter}와 같은 방식). */
@Converter
public class BadgeColorConverter implements AttributeConverter<BadgeColor, String> {

    @Override
    public String convertToDatabaseColumn(BadgeColor attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public BadgeColor convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BadgeColor.fromCode(dbData);
    }
}
