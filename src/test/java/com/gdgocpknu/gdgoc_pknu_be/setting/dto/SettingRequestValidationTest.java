package com.gdgocpknu.gdgoc_pknu_be.setting.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingRequest.RecruitPeriodRequest;
import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingRequest.StatRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Bean Validation만 단독으로 돌려 API 명세서 4-7 규칙을 확인한다. Spring 컨텍스트 · DB가 필요 없다. */
class SettingRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void 모든_필드를_채우면_통과한다() {
        assertThat(validator.validate(valid(r -> r))).isEmpty();
    }

    @Test
    void 모집_기간은_없어도_통과하고_둘_다_비어_있어도_통과한다() {
        assertThat(validator.validate(valid(r -> withPeriod(r, null)))).isEmpty();
        assertThat(validator.validate(valid(r -> withPeriod(r, new RecruitPeriodRequest(null, null))))).isEmpty();
    }

    @Test
    void 모집_기간은_하나만_입력하면_비어_있는_칸에_위반이_달린다() {
        SettingRequest onlyStart = valid(r -> withPeriod(r, new RecruitPeriodRequest(LocalDate.of(2026, 9, 1), null)));
        assertThat(fieldsOf(validator.validate(onlyStart))).containsExactly("recruitPeriod.end");

        SettingRequest onlyEnd = valid(r -> withPeriod(r, new RecruitPeriodRequest(null, LocalDate.of(2026, 9, 30))));
        assertThat(fieldsOf(validator.validate(onlyEnd))).containsExactly("recruitPeriod.start");
    }

    @Test
    void 종료일이_시작일보다_빠르면_end에_위반이_달리고_같은_날은_통과한다() {
        SettingRequest reversed = valid(r -> withPeriod(r,
                new RecruitPeriodRequest(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1))));
        assertThat(fieldsOf(validator.validate(reversed))).containsExactly("recruitPeriod.end");

        SettingRequest sameDay = valid(r -> withPeriod(r,
                new RecruitPeriodRequest(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1))));
        assertThat(validator.validate(sameDay)).isEmpty();
    }

    @Test
    void 통계는_정확히_4개여야_한다() {
        assertThat(fieldsOf(validator.validate(valid(r -> withStats(r, stats(3)))))).containsExactly("stats");
        assertThat(fieldsOf(validator.validate(valid(r -> withStats(r, stats(5)))))).containsExactly("stats");
        assertThat(fieldsOf(validator.validate(valid(r -> withStats(r, List.of()))))).containsExactly("stats");
        assertThat(fieldsOf(validator.validate(valid(r -> withStats(r, null))))).containsExactly("stats");
    }

    @Test
    void 통계_원소가_null이면_위반이다() {
        List<StatRequest> withNull = new ArrayList<>(stats(3));
        withNull.add(null);

        assertThat(fieldsOf(validator.validate(valid(r -> withStats(r, withNull)))))
                .containsExactly("stats[3].<list element>");
    }

    @Test
    void 통계의_label은_필수이고_글자_수_제한을_넘으면_위반이다() {
        List<StatRequest> bad = new ArrayList<>(stats(2));
        bad.add(new StatRequest("", 1, null, null));
        bad.add(new StatRequest("가".repeat(21), 1, "가".repeat(11), "가".repeat(6)));

        assertThat(fieldsOf(validator.validate(valid(r -> withStats(r, bad)))))
                .containsExactlyInAnyOrder("stats[2].label", "stats[3].label", "stats[3].unit", "stats[3].suffix");
    }

    @Test
    void 통계의_value는_필수이고_음수면_위반이며_0은_통과한다() {
        List<StatRequest> bad = new ArrayList<>(stats(2));
        bad.add(new StatRequest("누적 멤버", null, null, null));
        bad.add(new StatRequest("완료 프로젝트", -1, null, null));
        assertThat(fieldsOf(validator.validate(valid(r -> withStats(r, bad)))))
                .containsExactlyInAnyOrder("stats[2].value", "stats[3].value");

        List<StatRequest> zero = new ArrayList<>(stats(3));
        zero.add(new StatRequest("완료 프로젝트", 0, null, null));
        assertThat(validator.validate(valid(r -> withStats(r, zero)))).isEmpty();
    }

    @Test
    void 배너_문구는_필수이고_60자_이하다() {
        assertThat(fieldsOf(validator.validate(valid(r -> withBanner(r, ""))))).containsExactly("bannerText");
        assertThat(fieldsOf(validator.validate(valid(r -> withBanner(r, "가".repeat(61)))))).containsExactly("bannerText");
        assertThat(validator.validate(valid(r -> withBanner(r, "가".repeat(60))))).isEmpty();
    }

    @Test
    void 모집_상태가_없으면_위반이다() {
        SettingRequest request = valid(r -> new SettingRequest(null, r.bannerText(), r.recruitPeriod(), r.stats()));

        assertThat(fieldsOf(validator.validate(request))).containsExactly("recruiting");
    }

    private static Set<String> fieldsOf(Set<ConstraintViolation<SettingRequest>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    private static SettingRequest withPeriod(SettingRequest r, RecruitPeriodRequest period) {
        return new SettingRequest(r.recruiting(), r.bannerText(), period, r.stats());
    }

    private static SettingRequest withStats(SettingRequest r, List<StatRequest> stats) {
        return new SettingRequest(r.recruiting(), r.bannerText(), r.recruitPeriod(), stats);
    }

    private static SettingRequest withBanner(SettingRequest r, String bannerText) {
        return new SettingRequest(r.recruiting(), bannerText, r.recruitPeriod(), r.stats());
    }

    private static List<StatRequest> stats(int count) {
        List<StatRequest> stats = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            stats.add(new StatRequest("통계" + i, i, null, null));
        }
        return stats;
    }

    private static SettingRequest valid(UnaryOperator<SettingRequest> customizer) {
        return customizer.apply(new SettingRequest(
                true, "2026년 2학기 신입 부원을 모집합니다",
                new RecruitPeriodRequest(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14)),
                stats(4)));
    }
}
