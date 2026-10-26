package com.gdgocpknu.gdgoc_pknu_be.event.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** `@Enumerated(STRING)`은 `MEETING`을 저장해 DB CHECK(코드값 목록)와 맞지 않으므로 코드값으로 변환한다. */
@Converter
public class EventCategoryConverter implements AttributeConverter<EventCategory, String> {

    @Override
    public String convertToDatabaseColumn(EventCategory attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public EventCategory convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EventCategory.fromCode(dbData);
    }
}
