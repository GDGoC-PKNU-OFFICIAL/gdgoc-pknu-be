package com.gdgocpknu.gdgoc_pknu_be.about.controller;

import com.gdgocpknu.gdgoc_pknu_be.about.dto.AboutRequest;
import com.gdgocpknu.gdgoc_pknu_be.about.dto.AboutResponse;
import com.gdgocpknu.gdgoc_pknu_be.about.service.AboutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 About 조회 · 수정. 사이트 설정처럼 단일 레코드라 id · 등록 · 삭제가 없다. */
@RestController
@RequestMapping("/api/admin/about")
@RequiredArgsConstructor
public class AdminAboutController {

    private final AboutService aboutService;

    @GetMapping
    public AboutResponse get() {
        return aboutService.get();
    }

    @PutMapping
    public AboutResponse update(@Valid @RequestBody AboutRequest request) {
        return aboutService.update(request);
    }
}
