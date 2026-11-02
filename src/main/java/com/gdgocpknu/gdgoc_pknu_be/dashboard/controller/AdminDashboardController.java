package com.gdgocpknu.gdgoc_pknu_be.dashboard.controller;

import com.gdgocpknu.gdgoc_pknu_be.dashboard.dto.DashboardResponse;
import com.gdgocpknu.gdgoc_pknu_be.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 대시보드 요약(A-00). */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse get() {
        return dashboardService.get();
    }
}
