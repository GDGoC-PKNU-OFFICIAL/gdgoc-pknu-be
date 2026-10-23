package com.gdgocpknu.gdgoc_pknu_be.study.controller;

import com.gdgocpknu.gdgoc_pknu_be.common.dto.PageResponse;
import com.gdgocpknu.gdgoc_pknu_be.common.dto.SlugCheckResponse;
import com.gdgocpknu.gdgoc_pknu_be.common.support.PeriodStatus;
import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudyRequest;
import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudyResponse;
import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudySearchCond;
import com.gdgocpknu.gdgoc_pknu_be.study.service.StudyCommandService;
import com.gdgocpknu.gdgoc_pknu_be.study.service.StudyQueryService;
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

/** 관리자 CRUD(A-21 ~ A-23). */
@Validated
@RestController
@RequestMapping("/api/admin/studies")
@RequiredArgsConstructor
public class AdminStudyController {

    private final StudyQueryService studyQueryService;
    private final StudyCommandService studyCommandService;

    /**
     * `status`는 코드값(`past`)으로 받는다. `sort`는 `필드,방향` 형식이며
     * 허용 필드는 `periodStart` · `title` · `updatedAt` (API 명세서 1-6, 2-5).
     */
    @GetMapping
    public PageResponse<StudyResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) PeriodStatus status,
            @RequestParam(required = false) @Positive @Max(9999) Integer year,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive @Max(200) int size,
            @RequestParam(defaultValue = "periodStart,desc") String sort) {
        return studyQueryService.findAllForAdmin(new StudySearchCond(q, status, year), page, size, sort);
    }

    @GetMapping("/{id:\\d+}")
    public StudyResponse get(@PathVariable Long id) {
        return studyQueryService.getForAdmin(id);
    }

    @PostMapping
    public ResponseEntity<StudyResponse> create(@Valid @RequestBody StudyRequest request) {
        StudyResponse response = studyCommandService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/studies/" + response.id())).body(response);
    }

    @PutMapping("/{id:\\d+}")
    public StudyResponse update(@PathVariable Long id, @Valid @RequestBody StudyRequest request) {
        return studyCommandService.update(id, request);
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studyCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** 수정 화면에서는 `excludeId`로 자기 자신을 제외한다. */
    @GetMapping("/slug-check")
    public SlugCheckResponse slugCheck(@RequestParam String slug, @RequestParam(required = false) Long excludeId) {
        return new SlugCheckResponse(studyQueryService.isSlugAvailable(slug, excludeId));
    }
}
