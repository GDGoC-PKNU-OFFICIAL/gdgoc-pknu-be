package com.gdgocpknu.gdgoc_pknu_be.setting.mapper;

import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSetting;
import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Entity → 응답 DTO. 모집 기간은 시작 · 종료가 모두 있을 때만 객체로 내려간다(DB CHECK로 둘은 함께 있거나 없다). */
@Component
@RequiredArgsConstructor
public class SettingMapper {

    private final KstDates kstDates;

    public SettingResponse toResponse(SiteSetting setting) {
        SettingResponse.RecruitPeriod period = setting.getRecruitStart() == null
                ? null
                : new SettingResponse.RecruitPeriod(setting.getRecruitStart(), setting.getRecruitEnd());
        return new SettingResponse(
                setting.isRecruiting(),
                setting.getBannerText(),
                period,
                setting.getStats().stream()
                        .map(stat -> new SettingResponse.Stat(
                                stat.getLabel(), stat.getValue(), stat.getUnit(), stat.getSuffix()))
                        .toList(),
                kstDates.toKst(setting.getUpdatedAt()));
    }
}
