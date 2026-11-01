package com.gdgocpknu.gdgoc_pknu_be.setting.controller;

import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingRequest;
import com.gdgocpknu.gdgoc_pknu_be.setting.dto.SettingResponse;
import com.gdgocpknu.gdgoc_pknu_be.setting.service.SettingCommandService;
import com.gdgocpknu.gdgoc_pknu_be.setting.service.SettingQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 조회 · 수정(A-50). 단일 레코드라 id가 없고 등록 · 삭제도 없다. */
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingController {

    private final SettingQueryService settingQueryService;
    private final SettingCommandService settingCommandService;

    @GetMapping
    public SettingResponse get() {
        return settingQueryService.get();
    }

    @PutMapping
    public SettingResponse update(@Valid @RequestBody SettingRequest request) {
        return settingCommandService.update(request);
    }
}
