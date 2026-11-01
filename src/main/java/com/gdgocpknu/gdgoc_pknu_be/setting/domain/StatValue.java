package com.gdgocpknu.gdgoc_pknu_be.setting.domain;

/** 통계 한 칸의 값. 엔티티를 만들거나 바꿀 때 서비스가 넘기는 입력이다. */
public record StatValue(String label, int value, String unit, String suffix) {
}
