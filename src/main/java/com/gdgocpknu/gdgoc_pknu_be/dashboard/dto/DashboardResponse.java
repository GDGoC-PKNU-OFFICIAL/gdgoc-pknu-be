package com.gdgocpknu.gdgoc_pknu_be.dashboard.dto;

import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventResponse;
import java.time.OffsetDateTime;
import java.util.List;

/** API 명세서 3-6 Dashboard. */
public record DashboardResponse(
        boolean recruiting,
        Counts counts,
        List<EventResponse> upcomingEvents,
        List<RecentUpdateItem> recentUpdates) {

    public record Counts(PeriodCount projects, PeriodCount studies, EventCount events, MemberCount members) {
    }

    public record PeriodCount(long current, long past) {
    }

    public record EventCount(long upcoming, long past) {
    }

    public record MemberCount(long core, long senior) {
    }

    /** `resource`는 `projects` · `studies` · `events` · `members`이고, 멤버의 `title`은 이름이다. */
    public record RecentUpdateItem(String resource, Long id, String title, OffsetDateTime updatedAt) {
    }
}
