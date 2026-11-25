package com.gdgocpknu.gdgoc_pknu_be.about.controller;

import com.gdgocpknu.gdgoc_pknu_be.about.dto.AboutResponse;
import com.gdgocpknu.gdgoc_pknu_be.about.service.AboutService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 API. About 페이지(소개 · 활동 · 규칙 · FAQ)와 홈 What We Do 에 쓰인다. 아직 저장한 적이 없으면 404. */
@RestController
@RequestMapping("/api/about")
@RequiredArgsConstructor
public class AboutController {

    private final AboutService aboutService;

    @GetMapping
    public AboutResponse get() {
        return aboutService.get();
    }
}
