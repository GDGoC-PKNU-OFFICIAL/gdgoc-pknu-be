package com.gdgocpknu.gdgoc_pknu_be.about.service;

import com.gdgocpknu.gdgoc_pknu_be.about.domain.AboutActivity;
import com.gdgocpknu.gdgoc_pknu_be.about.domain.AboutFaq;
import com.gdgocpknu.gdgoc_pknu_be.about.domain.AboutPage;
import com.gdgocpknu.gdgoc_pknu_be.about.domain.AboutPageRepository;
import com.gdgocpknu.gdgoc_pknu_be.about.domain.AboutRule;
import com.gdgocpknu.gdgoc_pknu_be.about.dto.AboutRequest;
import com.gdgocpknu.gdgoc_pknu_be.about.dto.AboutRequest.ActivityRequest;
import com.gdgocpknu.gdgoc_pknu_be.about.dto.AboutResponse;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.storage.UploadTarget;
import com.gdgocpknu.gdgoc_pknu_be.storage.service.UploadVerifier;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * About 페이지 조회 · 수정. 단일 행이며, 처음 저장할 때 행이 생긴다.
 * 행이 없으면 404 — 프론트는 이때 코드에 들어 있는 기본 내용을 보여 준다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AboutService {

    private final AboutPageRepository aboutPageRepository;
    private final UploadVerifier uploadVerifier;
    private final KstDates kstDates;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AboutResponse get() {
        return aboutPageRepository.findById(AboutPage.SINGLETON_ID)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    public AboutResponse update(AboutRequest request) {
        requireUniqueRuleSlugs(request);

        AboutPage existing = aboutPageRepository.findById(AboutPage.SINGLETON_ID).orElse(null);
        verifyNewUploads(request.activities(), existing);

        List<String> identity = request.identity().stream().map(String::strip).toList();
        List<AboutActivity> activities = request.activities().stream().map(AboutService::toActivity).toList();
        List<AboutRule> rules = request.rules().stream()
                .map(rule -> new AboutRule(rule.slug(), rule.title().strip(), rule.icon(), rule.body().strip()))
                .toList();
        List<AboutFaq> faq = request.faq().stream()
                .map(item -> new AboutFaq(item.question().strip(), item.answer().strip()))
                .toList();

        AboutPage page;
        if (existing == null) {
            page = aboutPageRepository.save(AboutPage.create(identity, activities, rules, faq));
        } else {
            existing.update(identity, activities, rules, faq);
            page = existing;
        }
        page.touch(clock.instant());
        aboutPageRepository.flush();
        return toResponse(page);
    }

    private static AboutActivity toActivity(ActivityRequest request) {
        return new AboutActivity(
                request.group(),
                request.title().strip(),
                request.cadence().strip(),
                request.description().strip(),
                request.layout(),
                blankToNull(request.imageUrl()),
                blankToNull(request.secondImageUrl()),
                blankToNull(request.imagePosition()),
                request.tone());
    }

    /** 규칙 슬러그는 상세 카드 주소(?rule=)라 겹치면 안 된다. 두 번째로 나온 칸에 에러를 단다. */
    private static void requireUniqueRuleSlugs(AboutRequest request) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < request.rules().size(); i++) {
            if (!seen.add(request.rules().get(i).slug())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "rules[" + i + "].slug", "규칙 주소(slug)가 겹칩니다.");
            }
        }
    }

    /**
     * 새로 올린 사진만 R2에서 다시 확인한다 (명세 2-9). 이미 저장돼 있던 주소 · 사이트 기본 그림(/images/…)은 건너뛴다.
     * 확인할 주소가 about/ 업로드 형식이 아니면 UploadVerifier가 400으로 막는다.
     */
    private void verifyNewUploads(List<ActivityRequest> activities, AboutPage existing) {
        Set<String> saved = new HashSet<>();
        if (existing != null) {
            existing.getActivities().stream()
                    .flatMap(activity -> Stream.of(activity.imageUrl(), activity.secondImageUrl()))
                    .filter(Objects::nonNull)
                    .forEach(saved::add);
        }
        activities.stream()
                .flatMap(activity -> Stream.of(activity.imageUrl(), activity.secondImageUrl()))
                .filter(url -> url != null && url.startsWith("https://") && !saved.contains(url))
                .distinct()
                .forEach(url -> uploadVerifier.verify(url, UploadTarget.ABOUT));
    }

    private AboutResponse toResponse(AboutPage page) {
        return new AboutResponse(
                List.copyOf(page.getIdentity()),
                List.copyOf(page.getActivities()),
                List.copyOf(page.getRules()),
                List.copyOf(page.getFaq()),
                kstDates.toKst(page.getUpdatedAt()));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip();
    }
}
