package com.gdgocpknu.gdgoc_pknu_be.project.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.gdgocpknu.gdgoc_pknu_be.common.support.CodedEnum;

/**
 * 프로젝트 배지 색. 정해진 팔레트 안에서만 고른다 — 실제 색값(라이트 · 다크)은 프론트가 코드값에 매핑한다.
 * DB · JSON 모두 코드값(영문 소문자)을 쓴다.
 */
public enum BadgeColor implements CodedEnum {

    BLUE("blue"),
    RED("red"),
    YELLOW("yellow"),
    GREEN("green"),
    PURPLE("purple"),
    GRAY("gray");

    private final String code;

    BadgeColor(String code) {
        this.code = code;
    }

    @Override
    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static BadgeColor fromCode(String code) {
        for (BadgeColor color : values()) {
            if (color.code.equals(code)) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown badge color: " + code);
    }
}
