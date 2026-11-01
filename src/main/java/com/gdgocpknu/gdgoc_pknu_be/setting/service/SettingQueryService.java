package com.gdgocpknu.gdgoc_pknu_be.setting.service;

import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSetting;
import com.gdgocpknu.gdgoc_pknu_be.setting.domain.SiteSettingRepository;
import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingResponse;
import com.gdgocpknu.gdgoc_pknu_be.setting.mapper.SettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingQueryService {

    private final SiteSettingRepository siteSettingRepository;
    private final SettingMapper settingMapper;

    /** 공개 · 관리자가 같은 응답을 쓴다. 기본 행은 기동 시 만들어지므로(SiteSettingSeeder) 없으면 404다. */
    public SettingResponse get() {
        return settingMapper.toResponse(getOrThrow());
    }

    SiteSetting getOrThrow() {
        return siteSettingRepository.findById(SiteSetting.SINGLETON_ID)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
