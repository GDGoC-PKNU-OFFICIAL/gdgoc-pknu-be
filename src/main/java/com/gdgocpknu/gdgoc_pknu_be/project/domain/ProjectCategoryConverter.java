package com.gdgocpknu.gdgoc_pknu_be.project.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** `@Enumerated(STRING)`은 `TEAM_PROJECT`를 저장해 DB CHECK(코드값 목록)와 맞지 않으므로 코드값으로 변환한다. */
@Converter
public class ProjectCategoryConverter implements AttributeConverter<ProjectCategory, String> {

    @Override
    public String convertToDatabaseColumn(ProjectCategory attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public ProjectCategory convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ProjectCategory.fromCode(dbData);
    }
}
