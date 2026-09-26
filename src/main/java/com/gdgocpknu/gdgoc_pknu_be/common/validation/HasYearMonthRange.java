package com.gdgocpknu.gdgoc_pknu_be.common.validation;

/** `YYYY.MM` 문자열 두 개(시작·종료)를 갖는 요청 DTO가 구현한다. {@link PeriodOrder}가 이 인터페이스로 검증한다. */
public interface HasYearMonthRange {

    String start();

    String end();
}
