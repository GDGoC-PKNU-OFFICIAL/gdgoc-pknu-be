package com.gdgocpknu.gdgoc_pknu_be.dashboard.service;

import com.gdgocpknu.gdgoc_pknu_be.common.dto.RecentUpdate;
import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.common.support.PeriodStatus;
import com.gdgocpknu.gdgoc_pknu_be.dashboard.dto.DashboardResponse;
import com.gdgocpknu.gdgoc_pknu_be.dashboard.dto.DashboardResponse.Counts;
import com.gdgocpknu.gdgoc_pknu_be.dashboard.dto.DashboardResponse.EventCount;
import com.gdgocpknu.gdgoc_pknu_be.dashboard.dto.DashboardResponse.MemberCount;
import com.gdgocpknu.gdgoc_pknu_be.dashboard.dto.DashboardResponse.PeriodCount;
import com.gdgocpknu.gdgoc_pknu_be.dashboard.dto.DashboardResponse.RecentUpdateItem;
import com.gdgocpknu.gdgoc_pknu_be.event.service.EventQueryService;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;
import com.gdgocpknu.gdgoc_pknu_be.member.service.MemberQueryService;
import com.gdgocpknu.gdgoc_pknu_be.project.service.ProjectQueryService;
import com.gdgocpknu.gdgoc_pknu_be.setting.service.SettingQueryService;
import com.gdgocpknu.gdgoc_pknu_be.study.service.StudyQueryService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드(A-00). 각 도메인 QueryService를 조합하기만 하며 쓰기가 없다.
 * "오늘"을 한 번만 계산해 자정에 걸친 요청에서도 건수와 목록이 같은 날짜를 보게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int UPCOMING_EVENT_LIMIT = 3;
    private static final int RECENT_UPDATE_LIMIT = 5;

    private final SettingQueryService settingQueryService;
    private final ProjectQueryService projectQueryService;
    private final StudyQueryService studyQueryService;
    private final EventQueryService eventQueryService;
    private final MemberQueryService memberQueryService;
    private final KstDates kstDates;

    public DashboardResponse get() {
        LocalDate today = kstDates.today();
        LocalDate firstDayOfThisMonth = today.withDayOfMonth(1);

        Counts counts = new Counts(
                new PeriodCount(
                        projectQueryService.countByStatus(PeriodStatus.CURRENT, firstDayOfThisMonth),
                        projectQueryService.countByStatus(PeriodStatus.PAST, firstDayOfThisMonth)),
                new PeriodCount(
                        studyQueryService.countByStatus(PeriodStatus.CURRENT, firstDayOfThisMonth),
                        studyQueryService.countByStatus(PeriodStatus.PAST, firstDayOfThisMonth)),
                new EventCount(eventQueryService.countUpcoming(today), eventQueryService.countPast(today)),
                new MemberCount(
                        memberQueryService.countByStatus(MemberStatus.CORE),
                        memberQueryService.countByStatus(MemberStatus.SENIOR)));

        return new DashboardResponse(
                settingQueryService.isRecruiting(),
                counts,
                eventQueryService.findUpcoming(today, UPCOMING_EVENT_LIMIT),
                recentUpdates());
    }

    /** 도메인마다 최근 5건을 받아 합친 뒤 다시 5건으로 자른다. 전체 상위 5건은 반드시 이 안에 있다. */
    private List<RecentUpdateItem> recentUpdates() {
        return Stream.of(
                        projectQueryService.findRecentUpdates(),
                        studyQueryService.findRecentUpdates(),
                        eventQueryService.findRecentUpdates(),
                        memberQueryService.findRecentUpdates())
                .flatMap(List::stream)
                .sorted(Comparator.comparing(RecentUpdate::updatedAt).reversed())
                .limit(RECENT_UPDATE_LIMIT)
                .map(update -> new RecentUpdateItem(
                        update.resource(), update.id(), update.title(), kstDates.toKst(update.updatedAt())))
                .toList();
    }
}
