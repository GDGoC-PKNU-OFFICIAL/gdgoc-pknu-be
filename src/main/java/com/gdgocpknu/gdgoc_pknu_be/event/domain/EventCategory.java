package com.gdgocpknu.gdgoc_pknu_be.event.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.gdgocpknu.gdgoc_pknu_be.common.support.CodedEnum;

/** 일정 유형(캘린더 색상용, API 명세서 3-10). DB · JSON 모두 코드값을 쓰고, 화면 라벨은 프론트가 매핑한다. */
public enum EventCategory implements CodedEnum {

    MEETING("meeting"),
    SEMINAR("seminar"),
    PROJECT("project"),
    STUDY("study"),
    FESTIVAL("festival"),
    PRESENTATION("presentation"),
    MILESTONE("milestone");

    private final String code;

    EventCategory(String code) {
        this.code = code;
    }

    @Override
    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static EventCategory fromCode(String code) {
        for (EventCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown event category: " + code);
    }
}
