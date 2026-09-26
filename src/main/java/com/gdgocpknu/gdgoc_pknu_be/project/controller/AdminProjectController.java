package com.gdgocpknu.gdgoc_pknu_be.project.controller;

import com.gdgocpknu.gdgoc_pknu_be.common.dto.PageResponse;
import com.gdgocpknu.gdgoc_pknu_be.common.dto.SlugCheckResponse;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectRequest;
import com.gdgocpknu.gdgoc_pknu_be.project.dto.ProjectResponse;
import com.gdgocpknu.gdgoc_pknu_be.project.service.ProjectCommandService;
import com.gdgocpknu.gdgoc_pknu_be.project.service.ProjectQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 CRUD(A-11 ~ A-13). `q`·`status`·`category`·`year`·`sort` 동적 조건은 다음 조각에서 추가하고,
 * 지금은 목록 조회가 페이징만 지원한다.
 */
@Validated
@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
public class AdminProjectController {

    private final ProjectQueryService projectQueryService;
    private final ProjectCommandService projectCommandService;

    @GetMapping
    public PageResponse<ProjectResponse> list(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive @Max(200) int size) {
        return projectQueryService.findAllForAdmin(page, size);
    }

    @GetMapping("/{id:\\d+}")
    public ProjectResponse get(@PathVariable Long id) {
        return projectQueryService.getForAdmin(id);
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectCommandService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/projects/" + response.id())).body(response);
    }

    @PutMapping("/{id:\\d+}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projectCommandService.update(id, request);
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** 수정 화면에서는 `excludeId`로 자기 자신을 제외한다. */
    @GetMapping("/slug-check")
    public SlugCheckResponse slugCheck(@RequestParam String slug, @RequestParam(required = false) Long excludeId) {
        return new SlugCheckResponse(projectQueryService.isSlugAvailable(slug, excludeId));
    }
}
