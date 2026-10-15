package com.gdgocpknu.gdgoc_pknu_be.study.mapper;

import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.common.support.PeriodStatus;
import com.gdgocpknu.gdgoc_pknu_be.common.support.YearMonthText;
import com.gdgocpknu.gdgoc_pknu_be.study.domain.Study;
import com.gdgocpknu.gdgoc_pknu_be.study.dto.StudyResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Entity → 응답 DTO. 저장하지 않는 계산값(status)과 표기 변환(YYYY.MM, KST 오프셋)을 여기서 채운다. */
@Component
@RequiredArgsConstructor
public class StudyMapper {

    private final KstDates kstDates;

    public StudyResponse toResponse(Study study) {
        return toResponse(study, kstDates.firstDayOfThisMonth());
    }

    /** 한 응답 안에서 "이번 달" 기준이 흔들리지 않도록 기준일을 한 번만 계산한다. */
    public List<StudyResponse> toResponses(List<Study> studies) {
        LocalDate firstDayOfThisMonth = kstDates.firstDayOfThisMonth();
        return studies.stream().map(study -> toResponse(study, firstDayOfThisMonth)).toList();
    }

    /** 조회 조건(status 필터)과 응답의 status가 같은 기준일을 쓰도록, 기준일을 이미 계산한 호출자가 넘긴다. */
    public StudyResponse toResponse(Study study, LocalDate firstDayOfThisMonth) {
        LocalDate periodEnd = study.getPeriodEnd();
        return new StudyResponse(
                study.getId(),
                study.getSlug(),
                study.getTitle(),
                study.getSummary(),
                study.getTopic(),
                PeriodStatus.of(periodEnd, firstDayOfThisMonth),
                new StudyResponse.Period(
                        YearMonthText.format(study.getPeriodStart()),
                        periodEnd == null ? null : YearMonthText.format(periodEnd)),
                study.getLeader(),
                study.getParticipants().stream().map(participant -> participant.getName()).toList(),
                study.getFormat(),
                study.getOutputs().stream()
                        .map(output -> new StudyResponse.Output(output.getLabel(), output.getUrl()))
                        .toList(),
                kstDates.toKst(study.getCreatedAt()),
                kstDates.toKst(study.getUpdatedAt()));
    }
}
