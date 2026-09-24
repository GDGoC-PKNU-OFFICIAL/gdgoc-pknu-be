package com.gdgocpknu.gdgoc_pknu_be.project.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Bean Validation만 단독으로 돌려 API 명세서 4-2 규칙을 확인한다. Spring 컨텍스트·DB가 필요 없다. */
class ProjectRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void 모든_필드를_채우면_통과한다() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    @Test
    void 빈_배열은_통과하지만_배열_필드_자체가_없으면_위반이다() {
        ProjectRequest withEmptyTeam = valid(v -> v.team(List.of()));
        assertThat(fieldsOf(validator.validate(withEmptyTeam))).doesNotContain("team");

        ProjectRequest withoutFeatures = new ProjectRequest(
                "t", "slug", "s", ProjectCategory.OFFICIAL,
                new ProjectRequest.PeriodRequest("2026.03", null), null,
                List.of("desc"), List.of(), List.of(), null, List.of(),
                new ProjectRequest.LinksRequest(null, null));
        assertThat(fieldsOf(validator.validate(withoutFeatures))).contains("features");
    }

    @Test
    void 종료월이_시작월보다_빠르면_period_end에_위반이_달린다() {
        ProjectRequest request = valid(v -> v.period(new ProjectRequest.PeriodRequest("2026.09", "2026.03")));

        Set<ConstraintViolation<ProjectRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("period.end"));
    }

    @Test
    void 형식이_깨진_기간은_순서_검사_없이_형식_위반만_난다() {
        ProjectRequest request = valid(v -> v.period(new ProjectRequest.PeriodRequest("2026-03", null)));

        assertThat(fieldsOf(validator.validate(request))).contains("period.start");
    }

    @Test
    void 기술_스택이_중복되면_위반이다() {
        ProjectRequest request = valid(v -> v.techStack(List.of("Spring", " Spring ")));

        assertThat(fieldsOf(validator.validate(request))).contains("techStack");
    }

    @Test
    void 슬러그_형식이_어긋나면_위반이다() {
        ProjectRequest request = valid(v -> v.slug("Campus Map"));

        assertThat(fieldsOf(validator.validate(request))).contains("slug");
    }

    @Test
    void 팀원_이름이_비어_있으면_team_0_name에_위반이_달린다() {
        ProjectRequest request = valid(v -> v.team(List.of(new ProjectRequest.TeamMemberRequest("", "백엔드"))));

        assertThat(fieldsOf(validator.validate(request))).contains("team[0].name");
    }

    private static Set<String> fieldsOf(Set<ConstraintViolation<ProjectRequest>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    private static ProjectRequest valid() {
        return valid(v -> {
        });
    }

    /** 기본으로 전부 유효한 요청을 만든 뒤, 한 필드만 바꿔서 개별 규칙을 검증한다. */
    private static ProjectRequest valid(Consumer<Builder> customizer) {
        Builder builder = new Builder();
        customizer.accept(builder);
        return builder.build();
    }

    /** record는 필드 하나만 바꾸는 게 번거로워서 테스트 전용으로 얇은 빌더를 둔다. */
    private static final class Builder {
        String title = "캠퍼스 맵";
        String slug = "campus-map";
        String summary = "부경대 길찾기 웹앱";
        ProjectCategory category = ProjectCategory.TEAM_PROJECT;
        ProjectRequest.PeriodRequest period = new ProjectRequest.PeriodRequest("2026.03", "2026.06");
        String thumbnailUrl = null;
        List<String> description = List.of("소개 문단");
        List<ProjectRequest.TeamMemberRequest> team = List.of(new ProjectRequest.TeamMemberRequest("홍길동", "백엔드"));
        List<String> techStack = List.of("Next.js", "Spring Boot");
        List<String> features = List.of();
        List<String> outcomes = List.of();
        ProjectRequest.LinksRequest links = new ProjectRequest.LinksRequest("https://github.com/gdgoc-pknu/campus-map", null);

        Builder slug(String v) { this.slug = v; return this; }
        Builder period(ProjectRequest.PeriodRequest v) { this.period = v; return this; }
        Builder team(List<ProjectRequest.TeamMemberRequest> v) { this.team = v; return this; }
        Builder techStack(List<String> v) { this.techStack = v; return this; }

        ProjectRequest build() {
            return new ProjectRequest(title, slug, summary, category, period, thumbnailUrl,
                    description, team, techStack, features, outcomes, links);
        }
    }
}
