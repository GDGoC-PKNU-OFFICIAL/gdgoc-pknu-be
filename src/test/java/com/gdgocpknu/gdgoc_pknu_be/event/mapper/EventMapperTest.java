package com.gdgocpknu.gdgoc_pknu_be.event.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.Event;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventCategory;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EventMapperTest {

    private final EventMapper mapper = new EventMapper(new KstDates(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)));

    @Test
    void 엔티티를_명세_3_3_모양으로_변환한다() {
        EventResponse response = mapper.toResponse(event("19:00–21:00", "공학관 101호", "한 줄 요약"));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.title()).isEqualTo("GDGoC 세미나");
        assertThat(response.category()).isEqualTo(EventCategory.SEMINAR);
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 9, 21));
        assertThat(response.time()).isEqualTo("19:00–21:00");
        assertThat(response.place()).isEqualTo("공학관 101호");
        assertThat(response.summary()).isEqualTo("한 줄 요약");
        assertThat(response.sortOrder()).isEqualTo(2);
    }

    @Test
    void 빈_문자열로_만든_선택_필드는_null이다() {
        EventResponse response = mapper.toResponse(event("", "   ", null));

        assertThat(response.time()).isNull();
        assertThat(response.place()).isNull();
        assertThat(response.summary()).isNull();
    }

    @Test
    void 생성_수정_일시는_KST_오프셋이다() {
        EventResponse response = mapper.toResponse(event(null, null, null));

        assertThat(response.createdAt()).isEqualTo(OffsetDateTime.parse("2026-09-15T19:30:00+09:00"));
        assertThat(response.updatedAt()).isEqualTo(OffsetDateTime.parse("2026-09-16T09:00:00+09:00"));
    }

    private static Event event(String time, String place, String summary) {
        Event event = Event.builder()
                .title("GDGoC 세미나").category(EventCategory.SEMINAR)
                .date(LocalDate.of(2026, 9, 20)).endDate(LocalDate.of(2026, 9, 21))
                .time(time).place(place).summary(summary).sortOrder(2)
                .build();
        ReflectionTestUtils.setField(event, "id", 7L);
        ReflectionTestUtils.setField(event, "createdAt", Instant.parse("2026-09-15T10:30:00Z"));
        ReflectionTestUtils.setField(event, "updatedAt", Instant.parse("2026-09-16T00:00:00Z"));
        return event;
    }
}
