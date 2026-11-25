package com.gdgocpknu.gdgoc_pknu_be.about.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.gdgocpknu.gdgoc_pknu_be.about.dto.AboutRequest.ActivityRequest;
import com.gdgocpknu.gdgoc_pknu_be.about.dto.AboutRequest.FaqRequest;
import com.gdgocpknu.gdgoc_pknu_be.about.dto.AboutRequest.RuleRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AboutRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    static ActivityRequest activity(String layout, String imageUrl) {
        return framed(layout, imageUrl, null, null);
    }

    static ActivityRequest framed(String layout, String imageUrl, String position, Integer zoom) {
        return new ActivityRequest(
                "정기 세션", "설명", layout, imageUrl, null, position, zoom, null, null, "auto");
    }

    static AboutRequest valid() {
        return new AboutRequest(
                List.of("첫 문단"),
                List.of(activity("square", "/images/activities/session-photo.webp")),
                List.of(new RuleRequest("code-of-conduct", "행동 강령", "book", "## 제목\n- 항목")),
                List.of(new FaqRequest("질문", "답변")));
    }

    @Test
    void 모든_필드를_채우면_통과한다() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    @Test
    void 소개와_활동은_한_개_이상이어야_한다() {
        AboutRequest request = new AboutRequest(List.of(), List.of(), List.of(), List.of());

        assertThat(fieldsOf(validator.validate(request))).containsExactlyInAnyOrder("identity", "activities");
    }

    @Test
    void 정해진_선택지_밖의_값은_위반이다() {
        AboutRequest request = new AboutRequest(
                List.of("문단"),
                List.of(new ActivityRequest(
                        "제목", "설명", "poster", null, null, "가운데", null, "왼쪽", null, "pink")),
                List.of(new RuleRequest("Bad Slug", "규칙", "star", "본문")),
                List.of());

        assertThat(fieldsOf(validator.validate(request))).containsExactlyInAnyOrder(
                "activities[0].layout", "activities[0].imagePosition",
                "activities[0].secondImagePosition", "activities[0].tone", "rules[0].slug", "rules[0].icon");
    }

    @Test
    void 위치는_0에서_100퍼센트_배율은_100에서_300퍼센트만_받는다() {
        String photo = "/images/activities/a.webp";

        assertThat(validator.validate(withActivity(framed("cover", photo, "0% 100%", 100)))).isEmpty();
        assertThat(validator.validate(withActivity(framed("cover", photo, "35% 50%", 300)))).isEmpty();
        assertThat(fieldsOf(validator.validate(withActivity(framed("cover", photo, "101% 50%", null)))))
                .containsExactly("activities[0].imagePosition");
        assertThat(fieldsOf(validator.validate(withActivity(framed("cover", photo, null, 99)))))
                .containsExactly("activities[0].imageZoom");
        assertThat(fieldsOf(validator.validate(withActivity(framed("cover", photo, null, 301)))))
                .containsExactly("activities[0].imageZoom");
    }

    @Test
    void 이미지는_사이트_기본_그림이나_https_주소만_받는다() {
        AboutRequest bundled = withActivity(activity("cover", "/images/activities/a.webp"));
        AboutRequest uploaded = withActivity(activity("cover", "https://pub.r2.dev/about/x.webp"));
        AboutRequest script = withActivity(activity("cover", "javascript:alert(1)"));
        AboutRequest traversal = withActivity(activity("cover", "/etc/passwd"));

        assertThat(validator.validate(bundled)).isEmpty();
        assertThat(validator.validate(uploaded)).isEmpty();
        assertThat(fieldsOf(validator.validate(script))).containsExactly("activities[0].imageUrl");
        assertThat(fieldsOf(validator.validate(traversal))).containsExactly("activities[0].imageUrl");
    }

    private static AboutRequest withActivity(ActivityRequest activity) {
        AboutRequest r = valid();
        return new AboutRequest(r.identity(), List.of(activity), r.rules(), r.faq());
    }

    private static Set<String> fieldsOf(Set<ConstraintViolation<AboutRequest>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }
}
