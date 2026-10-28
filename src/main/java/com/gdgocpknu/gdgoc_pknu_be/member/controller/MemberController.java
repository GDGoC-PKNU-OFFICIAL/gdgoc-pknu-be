package com.gdgocpknu.gdgoc_pknu_be.member.controller;

import com.gdgocpknu.gdgoc_pknu_be.member.dto.MemberResponse;
import com.gdgocpknu.gdgoc_pknu_be.member.service.MemberQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 API. 인증 없이 읽기 전용이며, 공개 동의 확인 일시는 응답에 포함하지 않는다. */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberQueryService memberQueryService;

    @GetMapping
    public List<MemberResponse> list() {
        return memberQueryService.findAllForPublic();
    }
}
