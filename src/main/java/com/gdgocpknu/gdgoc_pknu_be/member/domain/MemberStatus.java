package com.gdgocpknu.gdgoc_pknu_be.member.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.gdgocpknu.gdgoc_pknu_be.common.support.CodedEnum;

/** 코어멤버 / 시니어 구분 (API 명세서 3-4). 시니어는 역할(role)이 없다. */
public enum MemberStatus implements CodedEnum {

    CORE("core"),
    SENIOR("senior");

    private final String code;

    MemberStatus(String code) {
        this.code = code;
    }

    @Override
    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static MemberStatus fromCode(String code) {
        for (MemberStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown MemberStatus: " + code);
    }
}
