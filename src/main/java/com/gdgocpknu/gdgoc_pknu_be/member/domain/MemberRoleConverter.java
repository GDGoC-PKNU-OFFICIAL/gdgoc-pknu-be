package com.gdgocpknu.gdgoc_pknu_be.member.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** `@Enumerated(STRING)`은 enum 이름을 저장해 DB CHECK(코드값 목록)와 맞지 않으므로 코드값으로 변환한다. */
@Converter
public class MemberRoleConverter implements AttributeConverter<MemberRole, String> {

    @Override
    public String convertToDatabaseColumn(MemberRole attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public MemberRole convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MemberRole.fromCode(dbData);
    }
}
