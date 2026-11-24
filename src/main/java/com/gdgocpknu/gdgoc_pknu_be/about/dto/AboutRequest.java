package com.gdgocpknu.gdgoc_pknu_be.about.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * About 페이지 수정 요청 (PUT 전체 교체). 구역마다 배열이며, 순서가 곧 화면 순서다.
 * 고정된 선택지(묶음 · 모양 · 색 · 아이콘)는 코드값 문자열로 받는다. 규칙 슬러그 중복은 서비스가 검사한다.
 */
public record AboutRequest(
        @NotNull @Size(min = 1, max = 5) List<@NotBlank @Size(max = 1000) String> identity,
        @NotNull @Size(min = 1, max = 12) List<@NotNull @Valid ActivityRequest> activities,
        @NotNull @Size(max = 6) List<@NotNull @Valid RuleRequest> rules,
        @NotNull @Size(max = 20) List<@NotNull @Valid FaqRequest> faq) {

    /** 업로드한 사진(https) 또는 사이트에 들어 있는 기본 그림(/images/…). 업로드 주소는 저장 시 R2에서 다시 확인한다. */
    static final String IMAGE_URL = "^(/images/[A-Za-z0-9._/-]+|https://\\S+)$";

    public record ActivityRequest(
            @NotBlank @Pattern(regexp = "activity|free-content", message = "묶음 값이 올바르지 않습니다.") String group,
            @NotBlank @Size(max = 30) String title,
            @NotBlank @Size(max = 20) String cadence,
            @NotBlank @Size(max = 200) String description,
            @NotBlank
            @Pattern(regexp = "color|cover|square|top|artwork|logo|circles", message = "카드 모양 값이 올바르지 않습니다.")
            String layout,
            @Pattern(regexp = IMAGE_URL, message = "이미지 주소가 올바르지 않습니다.") @Size(max = 500) String imageUrl,
            @Pattern(regexp = IMAGE_URL, message = "이미지 주소가 올바르지 않습니다.") @Size(max = 500) String secondImageUrl,
            @Pattern(regexp = "^\\d{1,3}% \\d{1,3}%$", message = "예: 35% 50%") String imagePosition,
            @NotBlank
            @Pattern(regexp = "auto|blue|green|coral|orange|navy|gray|purple|forest|plum", message = "색 값이 올바르지 않습니다.")
            String tone) {
    }

    public record RuleRequest(
            @NotBlank
            @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "영문 소문자·숫자·하이픈만 사용할 수 있습니다.")
            @Size(max = 60)
            String slug,
            @NotBlank @Size(max = 30) String title,
            @NotBlank @Pattern(regexp = "book|person|badge", message = "아이콘 값이 올바르지 않습니다.") String icon,
            @NotBlank @Size(max = 5000) String body) {
    }

    public record FaqRequest(
            @NotBlank @Size(max = 100) String question,
            @NotBlank @Size(max = 1000) String answer) {
    }
}
