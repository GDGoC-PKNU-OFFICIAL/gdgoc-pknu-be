package com.gdgocpknu.gdgoc_pknu_be.member.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.Member;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberPart;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRole;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.AdminMemberResponse;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MemberMapperTest {

    private final MemberMapper mapper = new MemberMapper(new KstDates(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)));

    @Test
    void 공개_응답은_명세_3_4_모양이고_공개_동의_일시를_담지_않는다() {
        MemberResponse response = mapper.toPublicResponse(core());

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.status()).isEqualTo(MemberStatus.CORE);
        assertThat(response.role()).isEqualTo(MemberRole.PART_LEAD);
        assertThat(response.part()).isEqualTo(MemberPart.AI_ML);
        assertThat(response.history()).containsExactly("2026 파트 리드");
        assertThat(response.interests()).containsExactly("LLM", "백엔드");
        assertThat(response.links()).isEqualTo(new MemberResponse.Links("https://github.com/gildong", null));
        assertThat(Arrays.stream(MemberResponse.class.getRecordComponents()).map(c -> c.getName()))
                .doesNotContain("consentConfirmedAt");
    }

    @Test
    void 관리자_응답에는_공개_동의_일시가_KST_오프셋으로_담긴다() {
        AdminMemberResponse response = mapper.toAdminResponse(core());

        assertThat(response.consentConfirmedAt()).isEqualTo(OffsetDateTime.parse("2026-09-15T19:30:00+09:00"));
        assertThat(response.createdAt()).isEqualTo(OffsetDateTime.parse("2026-09-15T19:30:00+09:00"));
        assertThat(response.updatedAt()).isEqualTo(OffsetDateTime.parse("2026-09-16T09:00:00+09:00"));
    }

    @Test
    void 시니어는_역할이_없고_링크가_비어_있어도_links_객체는_내려간다() {
        Member senior = Member.builder()
                .status(MemberStatus.SENIOR).name("이시니").department("컴퓨터공학과")
                .history(List.of()).interests(List.of()).part(MemberPart.BACKEND).bio("소개")
                .consentConfirmedAt(Instant.parse("2026-09-15T10:30:00Z"))
                .build();
        ReflectionTestUtils.setField(senior, "createdAt", Instant.parse("2026-09-15T10:30:00Z"));
        ReflectionTestUtils.setField(senior, "updatedAt", Instant.parse("2026-09-15T10:30:00Z"));

        MemberResponse response = mapper.toPublicResponse(senior);

        assertThat(response.role()).isNull();
        assertThat(response.photoUrl()).isNull();
        assertThat(response.links()).isEqualTo(new MemberResponse.Links(null, null));
    }

    private static Member core() {
        Member member = Member.builder()
                .status(MemberStatus.CORE).role(MemberRole.PART_LEAD).name("홍길동")
                .department("컴퓨터공학과").history(List.of("2026 파트 리드")).interests(List.of("LLM", "백엔드"))
                .part(MemberPart.AI_ML).bio("함께 만드는 개발자")
                .githubUrl("https://github.com/gildong")
                .consentConfirmedAt(Instant.parse("2026-09-15T10:30:00Z"))
                .build();
        ReflectionTestUtils.setField(member, "id", 5L);
        ReflectionTestUtils.setField(member, "createdAt", Instant.parse("2026-09-15T10:30:00Z"));
        ReflectionTestUtils.setField(member, "updatedAt", Instant.parse("2026-09-16T00:00:00Z"));
        return member;
    }
}
