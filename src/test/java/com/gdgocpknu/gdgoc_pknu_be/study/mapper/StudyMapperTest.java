package com.gdgocpknu.gdgoc_pknu_be.study.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.common.support.PeriodStatus;
import com.gdgocpknu.gdgoc_pknu_be.study.domain.Study;
import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudyResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class StudyMapperTest {

    /** 2026-09-20 (KST) */
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-09-20T03:00:00Z"), ZoneOffset.UTC);

    private final StudyMapper mapper = new StudyMapper(new KstDates(NOW));

    @Test
    void 엔티티를_명세_3_2_모양으로_변환한다() {
        Study study = study("spring-study", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 6, 1));
        study.addParticipant("김부경");
        study.addParticipant("이서울");
        study.addOutput("회고록", "https://velog.io/@gdgoc/retrospective");

        StudyResponse response = mapper.toResponse(study);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.slug()).isEqualTo("spring-study");
        assertThat(response.leader()).isEqualTo("홍길동");
        assertThat(response.period()).isEqualTo(new StudyResponse.Period("2025.03", "2025.06"));
        assertThat(response.participants()).containsExactly("김부경", "이서울");
        assertThat(response.outputs())
                .containsExactly(new StudyResponse.Output("회고록", "https://velog.io/@gdgoc/retrospective"));
        assertThat(response.format()).isEqualTo("주 1회 온라인");
    }

    @Test
    void 종료월이_없으면_period_end는_null이고_현재다() {
        StudyResponse response = mapper.toResponse(study("ongoing", LocalDate.of(2026, 3, 1), null));

        assertThat(response.period().end()).isNull();
        assertThat(response.status()).isEqualTo(PeriodStatus.CURRENT);
    }

    @Test
    void 종료월이_이번_달이면_현재_지난달이면_과거다() {
        assertThat(mapper.toResponse(study("a", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 1))).status())
                .isEqualTo(PeriodStatus.CURRENT);
        assertThat(mapper.toResponse(study("b", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 8, 1))).status())
                .isEqualTo(PeriodStatus.PAST);
    }

    @Test
    void 생성_수정_일시는_KST_오프셋이다() {
        StudyResponse response = mapper.toResponse(study("a", LocalDate.of(2026, 3, 1), null));

        assertThat(response.createdAt()).isEqualTo(OffsetDateTime.parse("2026-09-15T19:30:00+09:00"));
        assertThat(response.updatedAt()).isEqualTo(OffsetDateTime.parse("2026-09-16T09:00:00+09:00"));
    }

    @Test
    void 목록_변환은_각_항목의_상태를_같은_기준일로_계산한다() {
        List<StudyResponse> responses = mapper.toResponses(List.of(
                study("past", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 6, 1)),
                study("current", LocalDate.of(2026, 3, 1), null)));

        assertThat(responses).extracting(StudyResponse::status)
                .containsExactly(PeriodStatus.PAST, PeriodStatus.CURRENT);
    }

    private static Study study(String slug, LocalDate start, LocalDate end) {
        Study study = Study.builder()
                .slug(slug).title("스프링 스터디").summary("스프링 부트 기초부터 JPA까지").topic("Backend")
                .periodStart(start).periodEnd(end)
                .leader("홍길동").format("주 1회 온라인")
                .build();
        ReflectionTestUtils.setField(study, "id", 7L);
        ReflectionTestUtils.setField(study, "createdAt", Instant.parse("2026-09-15T10:30:00Z"));
        ReflectionTestUtils.setField(study, "updatedAt", Instant.parse("2026-09-16T00:00:00Z"));
        return study;
    }
}
