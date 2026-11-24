package com.gdgocpknu.gdgoc_pknu_be.about.domain;

/**
 * What We Do 카드 한 장 (about_page.activities 의 원소).
 *
 * @param group          묶음 — {@code activity}(GDGoC PKNU 활동, 홈에도 보임) · {@code free-content}(무료 학습 콘텐츠)
 * @param layout         카드 모양 — {@code color}(색 카드) · {@code cover}(사진 가득) · {@code square}(위 정사각형 사진)
 *                       · {@code top}(글자 위쪽만 사진) · {@code artwork}(색 카드 위 그림) · {@code logo}(색 카드 위 로고)
 *                       · {@code circles}(원형 사진 두 장)
 * @param imageUrl       사진 · 그림 주소. R2 업로드(about/) 또는 사이트에 들어 있는 기본 그림(/images/…)
 * @param secondImageUrl circles 의 작은 원 사진
 * @param imagePosition  cover · top 사진에서 보일 중심 (예: "35% 50%"). 없으면 가운데
 * @param tone           카드 바탕색. {@code auto}면 순서대로 돌아가는 기본 색
 */
public record AboutActivity(
        String group,
        String title,
        String cadence,
        String description,
        String layout,
        String imageUrl,
        String secondImageUrl,
        String imagePosition,
        String tone) {
}
