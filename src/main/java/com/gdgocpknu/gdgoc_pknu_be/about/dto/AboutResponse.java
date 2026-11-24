package com.gdgocpknu.gdgoc_pknu_be.about.dto;

import com.gdgocpknu.gdgoc_pknu_be.about.domain.AboutActivity;
import com.gdgocpknu.gdgoc_pknu_be.about.domain.AboutFaq;
import com.gdgocpknu.gdgoc_pknu_be.about.domain.AboutRule;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * About 페이지 내용. 공개(`GET /api/about`)와 관리자(`GET /api/admin/about`) 응답이 같다.
 * 활동 카드의 빈 선택 값(imageUrl 등)은 전역 설정(non_null)으로 생략된다.
 */
public record AboutResponse(
        List<String> identity,
        List<AboutActivity> activities,
        List<AboutRule> rules,
        List<AboutFaq> faq,
        OffsetDateTime updatedAt) {
}
