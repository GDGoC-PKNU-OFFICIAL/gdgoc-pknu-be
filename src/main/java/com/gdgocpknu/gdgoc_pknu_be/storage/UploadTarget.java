package com.gdgocpknu.gdgoc_pknu_be.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.gdgocpknu.gdgoc_pknu_be.common.support.CodedEnum;

/** 업로드 대상(API 명세서 4-8). 파일 키의 접두사와, 재검증 실패를 알릴 요청 필드를 정한다. */
public enum UploadTarget implements CodedEnum {

    PROJECT("project", "projects/", "thumbnailUrl"),
    MEMBER("member", "members/", "photoUrl"),
    /** About What We Do 카드 사진. 에러는 activities 칸에 단다 (어느 카드인지는 폼이 업로드 중인 칸으로 안다) */
    ABOUT("about", "about/", "activities"),
    /** 홈 히어로 배경 사진 (사이트 설정) */
    HERO("hero", "hero/", "heroImageUrl");

    private final String code;
    private final String keyPrefix;
    private final String requestField;

    UploadTarget(String code, String keyPrefix, String requestField) {
        this.code = code;
        this.keyPrefix = keyPrefix;
        this.requestField = requestField;
    }

    @Override
    @JsonValue
    public String code() {
        return code;
    }

    public String keyPrefix() {
        return keyPrefix;
    }

    public String requestField() {
        return requestField;
    }

    @JsonCreator
    public static UploadTarget fromCode(String code) {
        for (UploadTarget value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown UploadTarget: " + code);
    }
}
