package com.gdgocpknu.gdgoc_pknu_be.member.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** `@Enumerated(STRING)`은 enum 이름을 저장해 DB CHECK(코드값 목록)와 맞지 않으므로 코드값으로 변환한다. */
@Converter
public class MemberPartConverter implements AttributeConverter<MemberPart, String> {

    @Override
    public String convertToDatabaseColumn(MemberPart attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public MemberPart convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MemberPart.fromCode(dbData);
    }
}
