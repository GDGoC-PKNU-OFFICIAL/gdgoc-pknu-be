package com.gdgocpknu.gdgoc_pknu_be.common.validation;

import java.time.LocalDate;

/** 시작일(`date`)과 선택 종료일(`endDate`)을 갖는 요청 DTO가 구현한다. {@link DateOrder}가 이 인터페이스로 검증한다. */
public interface HasDateRange {

    LocalDate date();

    LocalDate endDate();
}
