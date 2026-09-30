package com.gdgocpknu.gdgoc_pknu_be.auth.controller;

import com.gdgocpknu.gdgoc_pknu_be.auth.dto.LoginRequest;
import com.gdgocpknu.gdgoc_pknu_be.auth.dto.LoginResponse;
import com.gdgocpknu.gdgoc_pknu_be.auth.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Next `POST /api/admin/login`만 이 API를 호출한다. 브라우저는 이 경로를 직접 부르지 않는다(PRD 4-3). */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return adminAuthService.login(request);
    }
}
