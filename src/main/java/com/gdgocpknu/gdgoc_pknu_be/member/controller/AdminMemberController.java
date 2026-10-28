package com.gdgocpknu.gdgoc_pknu_be.member.controller;

import com.gdgocpknu.gdgoc_pknu_be.common.dto.PageResponse;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberRole;
import com.gdgocpknu.gdgoc_pknu_be.member.domain.MemberStatus;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.AdminMemberResponse;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberRequest;
import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberSearchCond;
import com.gdgocpknu.gdgoc_pknu_be.member.service.MemberCommandService;
import com.gdgocpknu.gdgoc_pknu_be.member.service.MemberQueryService;
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

/** 관리자 CRUD(A-41 ~ A-43). 멤버는 슬러그가 없어 slug-check가 없다. */
@Validated
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;

    /**
     * `status` · `role`은 코드값(`core`, `part-lead`)으로 받는다. `sort`는 `필드,방향` 형식이며 허용 필드는
     * `name` · `updatedAt` (API 명세서 1-6, 2-7). 보내지 않으면 구분 → 역할 순 → 이름 기본 정렬이다.
     */
    @GetMapping
    public PageResponse<AdminMemberResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) MemberStatus status,
            @RequestParam(required = false) MemberRole role,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive @Max(200) int size,
            @RequestParam(required = false) String sort) {
        return memberQueryService.findAllForAdmin(new MemberSearchCond(q, status, role), page, size, sort);
    }

    @GetMapping("/{id:\\d+}")
    public AdminMemberResponse get(@PathVariable Long id) {
        return memberQueryService.getForAdmin(id);
    }

    @PostMapping
    public ResponseEntity<AdminMemberResponse> create(@Valid @RequestBody MemberRequest request) {
        AdminMemberResponse response = memberCommandService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/members/" + response.id())).body(response);
    }

    @PutMapping("/{id:\\d+}")
    public AdminMemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return memberCommandService.update(id, request);
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
