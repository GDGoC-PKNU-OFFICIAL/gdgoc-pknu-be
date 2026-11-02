package com.gdgocpknu.gdgoc_pknu_be.storage.controller;

import com.gdgocpknu.gdgoc_pknu_be.storage.dto.PresignRequest;
import com.gdgocpknu.gdgoc_pknu_be.storage.dto.PresignResponse;
import com.gdgocpknu.gdgoc_pknu_be.storage.service.PresignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 이미지 업로드 주소 발급(A-12, A-42). 파일은 브라우저가 R2로 직접 올린다. */
@RestController
@RequestMapping("/api/admin/uploads")
@RequiredArgsConstructor
public class AdminUploadController {

    private final PresignService presignService;

    @PostMapping("/presign")
    public PresignResponse presign(@Valid @RequestBody PresignRequest request) {
        return presignService.presign(request);
    }
}
