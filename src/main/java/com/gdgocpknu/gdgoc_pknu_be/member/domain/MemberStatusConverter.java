package com.gdgocpknu.gdgoc_pknu_be.member.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** `@Enumerated(STRING)`은 enum 이름을 저장해 DB CHECK(코드값 목록)와 맞지 않으므로 코드값으로 변환한다. */
@Converter
public class MemberStatusConverter implements AttributeConverter<MemberStatus, String> {

    @Override
    public String convertToDatabaseColumn(MemberStatus attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public MemberStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MemberStatus.fromCode(dbData);
    }
}
