package com.gdgocpknu.gdgoc_pknu_be.project.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 프로젝트 유형. DB · JSON 모두 코드값(영문 소문자 + 하이픈)을 쓰고, 화면 라벨은 프론트가 매핑한다. */
public enum ProjectCategory {

    SOLUTION_CHALLENGE("solution-challenge"),
    TEAM_PROJECT("team-project"),
    OFFICIAL("official");

    private final String code;

    ProjectCategory(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static ProjectCategory fromCode(String code) {
        for (ProjectCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown project category: " + code);
    }
}
