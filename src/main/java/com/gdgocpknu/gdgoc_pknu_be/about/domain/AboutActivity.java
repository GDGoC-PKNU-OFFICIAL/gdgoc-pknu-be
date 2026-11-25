package com.gdgocpknu.gdgoc_pknu_be.about.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * What We Do 카드 한 장 (about_page.activities 의 원소). 목록 순서가 곧 About · 홈 화면 순서다.
 * 모르는 키는 무시한다 — 예전에 저장한 행에 남아 있는 키(없앤 group · cadence 등)가 있어도 읽힌다.
 *
 * @param layout              카드 모양 — {@code color}(색 카드) · {@code cover}(사진 가득) · {@code square}(위 정사각형 사진)
 *                            · {@code top}(글자 위쪽만 사진) · {@code artwork}(색 카드 위 그림) · {@code logo}(색 카드 위 로고)
 *                            · {@code circles}(원형 사진 두 장)
 * @param imageUrl            사진 · 그림 주소. R2 업로드(about/) 또는 사이트에 들어 있는 기본 그림(/images/…)
 * @param secondImageUrl      circles 의 작은 원 사진
 * @param imagePosition       사진에서 카드에 보일 곳 (예: "35% 50%" = CSS object-position). 없으면 모양마다 정한 기본 위치
 * @param imageZoom           사진 확대 배율 % (100~300). 없으면 100 — 확대 기준점은 imagePosition
 * @param secondImagePosition circles 의 작은 원 사진에서 보일 곳
 * @param secondImageZoom     circles 의 작은 원 사진 확대 배율 %
 * @param tone                카드 바탕색. {@code auto}면 순서대로 돌아가는 기본 색
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AboutActivity(
        String title,
        String description,
        String layout,
        String imageUrl,
        String secondImageUrl,
        String imagePosition,
        Integer imageZoom,
        String secondImagePosition,
        Integer secondImageZoom,
        String tone) {
}
