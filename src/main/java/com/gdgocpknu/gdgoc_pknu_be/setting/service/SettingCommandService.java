package com.gdgocpknu.gdgoc_pknu_be.setting.service;

import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSetting;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSettingRepository;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.StatValue;
import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingRequest;
import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingRequest.RecruitPeriodRequest;
import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingResponse;
import com.gdgocpknu.gdgoc_pknu_be.setting.mapper.SettingMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사이트 설정 수정(A-50). 단일 레코드를 통째로 교체한다. 기본 행은 기동 시 만들어지지만, 없는 환경에서도
 * 관리자가 저장하면 그 행이 만들어지도록 "없으면 생성"으로 동작한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SettingCommandService {

    private final SiteSettingRepository siteSettingRepository;
    private final SettingMapper settingMapper;
    private final Clock clock;

    public SettingResponse update(SettingRequest request) {
        RecruitPeriodRequest period = request.recruitPeriod();
        LocalDate start = period == null ? null : period.start();
        LocalDate end = period == null ? null : period.end();
        List<StatValue> stats = request.stats().stream()
                .map(stat -> new StatValue(stat.label(), stat.value(), stat.unit(), stat.suffix()))
                .toList();

        SiteSetting setting = siteSettingRepository.findById(SiteSetting.SINGLETON_ID)
                .map(existing -> {
                    existing.update(request.recruiting(), request.bannerText(), start, end, stats);
                    return existing;
                })
                .orElseGet(() -> siteSettingRepository.save(
                        SiteSetting.create(request.recruiting(), request.bannerText(), start, end, stats)));
        // 통계 값만 바뀐 수정은 site_setting 행이 dirty하지 않아 auditing이 updatedAt을 갱신하지 않는다.
        setting.touch(clock.instant());

        // updatedAt(@LastModifiedDate)은 flush 시점에 채워지므로, 응답을 만들기 전에 flush해야 새 값이 나간다.
        siteSettingRepository.flush();
        return settingMapper.toResponse(setting);
    }
}
