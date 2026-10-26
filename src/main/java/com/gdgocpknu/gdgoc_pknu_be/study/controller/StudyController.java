package com.gdgocpknu.gdgoc_pknu_be.study.controller;

import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudyResponse;
import com.gdgocpknu.gdgoc_pknu_be.study.service.StudyQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 API. 인증 없이 읽기 전용이며, Next.js가 빌드 · ISR 시점에만 호출한다. */
@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyController {

    private final StudyQueryService studyQueryService;

    @GetMapping
    public List<StudyResponse> list() {
        return studyQueryService.findAllForPublic();
    }
}
