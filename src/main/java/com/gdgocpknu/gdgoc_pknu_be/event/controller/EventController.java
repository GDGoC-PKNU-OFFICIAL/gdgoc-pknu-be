package com.gdgocpknu.gdgoc_pknu_be.event.controller;

import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventResponse;
import com.gdgocpknu.gdgoc_pknu_be.event.service.EventQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 API. 인증 없이 읽기 전용이며, Next.js가 빌드 · ISR 시점에만 호출한다. */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventQueryService eventQueryService;

    @GetMapping
    public List<EventResponse> list() {
        return eventQueryService.findAllForPublic();
    }
}
