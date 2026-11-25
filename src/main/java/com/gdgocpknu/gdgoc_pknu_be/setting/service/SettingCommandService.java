package com.gdgocpknu.gdgoc_pknu_be.setting.service;

import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSetting;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSettingRepository;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.StatValue;
import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingRequest;
import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingRequest.RecruitPeriodRequest;
import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingResponse;
import com.gdgocpknu.gdgoc_pknu_be.setting.mapper.SettingMapper;
import com.gdgocpknu.gdgoc_pknu_be.storage.UploadTarget;
import com.gdgocpknu.gdgoc_pknu_be.storage.service.UploadVerifier;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
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
    private final UploadVerifier uploadVerifier;
    private final Clock clock;

    public SettingResponse update(SettingRequest request) {
        RecruitPeriodRequest period = request.recruitPeriod();
        LocalDate start = period == null ? null : period.start();
        LocalDate end = period == null ? null : period.end();
        List<StatValue> stats = request.stats().stream()
                .map(stat -> new StatValue(stat.label(), stat.value()))
                .toList();

        SiteSetting existingSetting = siteSettingRepository.findById(SiteSetting.SINGLETON_ID).orElse(null);
        // 히어로 사진은 새로 올라왔거나 바뀐 경우에만 R2에서 다시 확인한다 (명세 2-9)
        String heroUrl = blankToNull(request.heroImageUrl());
        String currentHero = existingSetting == null ? null : existingSetting.getHeroImageUrl();
        if (heroUrl != null && !Objects.equals(heroUrl, currentHero)) {
            uploadVerifier.verify(heroUrl, UploadTarget.HERO);
        }

        SiteSetting setting = java.util.Optional.ofNullable(existingSetting)
                .map(existing -> {
                    existing.update(request.recruiting(), start, end, stats);
                    return existing;
                })
                .orElseGet(() -> siteSettingRepository.save(
                        SiteSetting.create(request.recruiting(), start, end, stats)));
        setting.updateHero(heroUrl, blankToNull(request.heroImagePosition()));
        // 통계 값만 바뀐 수정은 site_setting 행이 dirty하지 않아 auditing이 updatedAt을 갱신하지 않는다.
        setting.touch(clock.instant());

        // updatedAt(@LastModifiedDate)은 flush 시점에 채워지므로, 응답을 만들기 전에 flush해야 새 값이 나간다.
        siteSettingRepository.flush();
        return settingMapper.toResponse(setting);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
