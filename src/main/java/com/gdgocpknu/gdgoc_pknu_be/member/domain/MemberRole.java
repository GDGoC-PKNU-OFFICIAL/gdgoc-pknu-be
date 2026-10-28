package com.gdgocpknu.gdgoc_pknu_be.member.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.gdgocpknu.gdgoc_pknu_be.common.support.CodedEnum;

/** 코어멤버의 역할 (API 명세서 3-4). 선언 순서가 표시 순서(리드 → 파트 리드 → 코어)다. */
public enum MemberRole implements CodedEnum {

    LEAD("lead"),
    PART_LEAD("part-lead"),
    CORE("core");

    private final String code;

    MemberRole(String code) {
        this.code = code;
    }

    @Override
    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static MemberRole fromCode(String code) {
        for (MemberRole value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown MemberRole: " + code);
    }
}
