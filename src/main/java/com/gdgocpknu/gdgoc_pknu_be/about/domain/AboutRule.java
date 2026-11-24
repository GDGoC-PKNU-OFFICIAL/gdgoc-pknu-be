package com.gdgocpknu.gdgoc_pknu_be.about.domain;

/**
 * 활동 규칙 문서 한 개 (about_page.rules 의 원소). 블록을 누르면 `?rule={slug}` 상세 카드에 본문이 열린다.
 *
 * @param icon 블록 아이콘 — {@code book} · {@code person} · {@code badge}
 * @param body 본문 마크다운. 프론트가 쓰는 문법만 해석한다: `## 소제목`, 문단, `- 목록`(두 칸 들여 쓰면 하위 목록),
 *             `1. 번호 목록`, `**굵게**`, `> 안내 상자`
 */
public record AboutRule(String slug, String title, String icon, String body) {
}
