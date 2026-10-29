package com.gdgocpknu.gdgoc_pknu_be.setting.controller;

import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingResponse;
import com.gdgocpknu.gdgoc_pknu_be.setting.service.SettingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 API. 인증 없이 읽기 전용이며, 홈의 모집 상태 배너와 활동 통계에 쓰인다. */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingQueryService settingQueryService;

    @GetMapping
    public SettingResponse get() {
        return settingQueryService.get();
    }
}
