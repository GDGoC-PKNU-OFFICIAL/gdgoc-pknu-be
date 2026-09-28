package com.gdgocpknu.gdgoc_pknu_be.common.support;

/**
 * API · DB에서 코드값(영문 소문자 + 하이픈, 예: `team-project`)으로 주고받는 enum (API 명세서 1-5).
 * 이 인터페이스를 구현하면 쿼리 파라미터(`?status=past`)도 enum 이름이 아니라 코드값으로 바인딩된다 (WebConfig).
 */
public interface CodedEnum {

    String code();
}
