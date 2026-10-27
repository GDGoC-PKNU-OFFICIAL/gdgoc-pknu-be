package com.gdgocpknu.gdgoc_pknu_be.member.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.gdgocpknu.gdgoc_pknu_be.common.support.CodedEnum;

/** 멤버의 분야 (API 명세서 3-4). */
public enum MemberPart implements CodedEnum {

    FRONTEND("frontend"),
    BACKEND("backend"),
    MOBILE("mobile"),
    AI_ML("ai-ml"),
    DESIGN("design");

    private final String code;

    MemberPart(String code) {
        this.code = code;
    }

    @Override
    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static MemberPart fromCode(String code) {
        for (MemberPart value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown MemberPart: " + code);
    }
}
