package com.gdgocpknu.gdgoc_pknu_be.study.service;

import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.common.support.YearMonthText;
import com.gdgocpknu.gdgoc_pknu_be.study.domain.Study;
import com.gdgocpknu.gdgoc_pknu_be.study.domain.StudyRepository;
import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudyRequest;
import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudyResponse;
import com.gdgocpknu.gdgoc_pknu_be.study.mapper.StudyMapper;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 등록 · 수정 · 삭제(A-22, A-23, 목록 삭제). 슬러그 중복은 저장 전에 먼저 검사하고(도메인 규칙),
 * 동시 저장으로 이 검사를 통과한 경우는 DB UNIQUE 위반을 GlobalExceptionHandler가 409로 변환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StudyCommandService {

    private final StudyRepository studyRepository;
    private final StudyQueryService studyQueryService;
    private final StudyMapper studyMapper;
    private final Clock clock;

    public StudyResponse create(StudyRequest request) {
        requireSlugAvailable(request.slug(), null);

        Study study = Study.builder()
                .slug(request.slug())
                .title(request.title())
                .summary(request.summary())
                .topic(request.topic())
                .periodStart(YearMonthText.parse(request.period().start()))
                .periodEnd(YearMonthText.parse(request.period().end()))
                .leader(request.leader())
                .format(request.format())
                .build();
        applyChildren(study, request);

        return studyMapper.toResponse(studyRepository.save(study));
    }

    /** 전체 필드 교체. 참여자 · 산출물도 `clear()` 후 다시 채우는 방식으로 통째로 바뀐다. */
    public StudyResponse update(Long id, StudyRequest request) {
        Study study = studyQueryService.getOrThrow(id);
        requireSlugAvailable(request.slug(), id);

        LocalDate periodStart = YearMonthText.parse(request.period().start());
        LocalDate periodEnd = YearMonthText.parse(request.period().end());
        study.updateContent(
                request.slug(), request.title(), request.summary(), request.topic(),
                periodStart, periodEnd, request.leader(), request.format());
        study.clearParticipants();
        study.clearOutputs();
        applyChildren(study, request);
        // 참여자 · 산출물만 바뀐 수정은 study 행이 dirty하지 않아 auditing이 updatedAt을 갱신하지 않는다.
        study.touch(clock.instant());

        // updatedAt(@LastModifiedDate)은 flush 시점에 채워지므로, 응답을 만들기 전에 flush해야 새 값이 나간다.
        studyRepository.flush();
        return studyMapper.toResponse(study);
    }

    /** 참여자 · 산출물 행은 `ON DELETE CASCADE`로 함께 삭제된다. */
    public void delete(Long id) {
        if (!studyRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        studyRepository.deleteById(id);
    }

    private void requireSlugAvailable(String slug, Long excludeId) {
        if (!studyQueryService.isSlugAvailable(slug, excludeId)) {
            throw new BusinessException(ErrorCode.SLUG_DUPLICATED, "slug", ErrorCode.SLUG_DUPLICATED.message());
        }
    }

    private static void applyChildren(Study study, StudyRequest request) {
        request.participants().forEach(study::addParticipant);
        request.outputs().forEach(output -> study.addOutput(output.label(), output.url()));
    }
}
