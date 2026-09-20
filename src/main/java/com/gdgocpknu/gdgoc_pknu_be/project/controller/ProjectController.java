package com.gdgocpknu.gdgoc_pknu_be.project.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectResponse;
import com.gdgocpknu.gdgoc_pknu_be.project.service.ProjectQueryService;

/** 공개 API. 인증 없이 읽기 전용이며, Next.js가 빌드 · ISR 시점에만 호출한다. */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectQueryService projectQueryService;

    @GetMapping
    public List<ProjectResponse> list() {
        return projectQueryService.findAllForPublic();
    }
}
