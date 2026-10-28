package com.gdgocpknu.gdgoc_pknu_be.member.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberPart;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRole;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberRequest.LinksRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Bean Validation만 단독으로 돌려 API 명세서 4-6 규칙을 확인한다. Spring 컨텍스트 · DB가 필요 없다. */
class MemberRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void 코어멤버와_시니어의_정상_요청은_통과한다() {
        assertThat(validator.validate(core(r -> r))).isEmpty();
        assertThat(validator.validate(senior(r -> r))).isEmpty();
    }

    @Test
    void 코어멤버에_역할이_없으면_role에_위반이_달린다() {
        MemberRequest request = core(r -> withRole(r, null));

        assertThat(fieldsOf(validator.validate(request))).containsExactly("role");
    }

    @Test
    void 시니어에_역할이_있으면_role에_위반이_달린다() {
        MemberRequest request = senior(r -> withRole(r, MemberRole.LEAD));

        assertThat(fieldsOf(validator.validate(request))).containsExactly("role");
    }

    @Test
    void 구분이_없으면_구분만_위반이고_역할_검사는_건너뛴다() {
        MemberRequest request = core(r -> new MemberRequest(null, null, r.name(), r.photoUrl(), r.department(),
                r.history(), r.interests(), r.part(), r.bio(), r.links(), r.consentConfirmed()));

        assertThat(fieldsOf(validator.validate(request))).containsExactly("status");
    }

    @Test
    void 공개_동의가_false이거나_없으면_consentConfirmed에_위반이_달린다() {
        assertThat(fieldsOf(validator.validate(core(r -> withConsent(r, false))))).containsExactly("consentConfirmed");
        assertThat(fieldsOf(validator.validate(core(r -> withConsent(r, null))))).containsExactly("consentConfirmed");
    }

    @Test
    void 주요_이력은_2개까지이고_원소가_비어_있거나_40자를_넘으면_위반이다() {
        MemberRequest three = core(r -> withHistory(r, List.of("가", "나", "다")));
        assertThat(fieldsOf(validator.validate(three))).contains("history");

        MemberRequest tooLong = core(r -> withHistory(r, List.of("가".repeat(41))));
        assertThat(fieldsOf(validator.validate(tooLong))).contains("history[0].<list element>");

        List<String> withBlankAndNull = new ArrayList<>(List.of(" "));
        withBlankAndNull.add(null);
        MemberRequest bad = core(r -> withHistory(r, withBlankAndNull));
        assertThat(fieldsOf(validator.validate(bad)))
                .contains("history[0].<list element>", "history[1].<list element>");
    }

    @Test
    void 관심분야는_3개까지이고_중복_불가이며_원소는_15자_이하다() {
        MemberRequest four = core(r -> withInterests(r, List.of("a", "b", "c", "d")));
        assertThat(fieldsOf(validator.validate(four))).contains("interests");

        MemberRequest duplicated = core(r -> withInterests(r, List.of("Spring", " Spring ")));
        assertThat(fieldsOf(validator.validate(duplicated))).contains("interests");

        MemberRequest tooLong = core(r -> withInterests(r, List.of("가".repeat(16))));
        assertThat(fieldsOf(validator.validate(tooLong))).contains("interests[0].<list element>");
    }

    @Test
    void 빈_배열은_통과하지만_배열_필드_자체가_없으면_위반이다() {
        assertThat(validator.validate(core(r -> withInterests(withHistory(r, List.of()), List.of())))).isEmpty();

        MemberRequest missing = core(r -> new MemberRequest(r.status(), r.role(), r.name(), r.photoUrl(),
                r.department(), null, null, r.part(), r.bio(), r.links(), r.consentConfirmed()));
        assertThat(fieldsOf(validator.validate(missing))).containsExactlyInAnyOrder("history", "interests");
    }

    @Test
    void 글자_수_제한을_넘으면_위반이다() {
        MemberRequest request = core(r -> new MemberRequest(r.status(), r.role(), "가".repeat(21), r.photoUrl(),
                "가".repeat(31), r.history(), r.interests(), r.part(), "가".repeat(61), r.links(),
                r.consentConfirmed()));

        assertThat(fieldsOf(validator.validate(request))).containsExactlyInAnyOrder("name", "department", "bio");
    }

    @Test
    void 링크와_사진은_http_s_형식이어야_한다() {
        MemberRequest request = core(r -> new MemberRequest(r.status(), r.role(), r.name(), "ftp://x/y.png",
                r.department(), r.history(), r.interests(), r.part(), r.bio(),
                new LinksRequest("github.com/me", "javascript:alert(1)"), r.consentConfirmed()));

        assertThat(fieldsOf(validator.validate(request)))
                .containsExactlyInAnyOrder("photoUrl", "links.github", "links.homepage");
    }

    @Test
    void links가_없으면_위반이다() {
        MemberRequest request = core(r -> new MemberRequest(r.status(), r.role(), r.name(), r.photoUrl(),
                r.department(), r.history(), r.interests(), r.part(), r.bio(), null, r.consentConfirmed()));

        assertThat(fieldsOf(validator.validate(request))).containsExactly("links");
    }

    private static Set<String> fieldsOf(Set<ConstraintViolation<MemberRequest>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    private static MemberRequest withRole(MemberRequest r, MemberRole role) {
        return new MemberRequest(r.status(), role, r.name(), r.photoUrl(), r.department(), r.history(),
                r.interests(), r.part(), r.bio(), r.links(), r.consentConfirmed());
    }

    private static MemberRequest withConsent(MemberRequest r, Boolean consent) {
        return new MemberRequest(r.status(), r.role(), r.name(), r.photoUrl(), r.department(), r.history(),
                r.interests(), r.part(), r.bio(), r.links(), consent);
    }

    private static MemberRequest withHistory(MemberRequest r, List<String> history) {
        return new MemberRequest(r.status(), r.role(), r.name(), r.photoUrl(), r.department(), history,
                r.interests(), r.part(), r.bio(), r.links(), r.consentConfirmed());
    }

    private static MemberRequest withInterests(MemberRequest r, List<String> interests) {
        return new MemberRequest(r.status(), r.role(), r.name(), r.photoUrl(), r.department(), r.history(),
                interests, r.part(), r.bio(), r.links(), r.consentConfirmed());
    }

    private static MemberRequest core(UnaryOperator<MemberRequest> customizer) {
        return customizer.apply(new MemberRequest(
                MemberStatus.CORE, MemberRole.PART_LEAD, "홍길동", "https://example.r2.dev/members/a.webp", "컴퓨터공학과",
                List.of("2026 GDGoC PKNU 파트 리드"), List.of("백엔드", "클라우드"), MemberPart.BACKEND,
                "함께 만드는 개발자", new LinksRequest("https://github.com/gildong", null), true));
    }

    private static MemberRequest senior(UnaryOperator<MemberRequest> customizer) {
        return customizer.apply(new MemberRequest(
                MemberStatus.SENIOR, null, "이시니", null, "컴퓨터공학과",
                List.of(), List.of("클라우드"), MemberPart.BACKEND,
                "졸업해도 연결되는 개발자", new LinksRequest(null, null), true));
    }
}
