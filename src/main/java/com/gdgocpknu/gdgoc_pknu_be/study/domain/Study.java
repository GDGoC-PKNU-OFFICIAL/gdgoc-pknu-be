package com.gdgocpknu.gdgoc_pknu_be.study.domain;

import com.gdgocpknu.gdgoc_pknu_be.common.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스터디. 현재/과거 status는 저장하지 않고 period_end로 조회 시 계산한다(프로젝트와 같은 규칙, PeriodStatus).
 * 리더 · 참여자는 멤버 테이블과 연결하지 않는 이름 문자열이다(PRD 5-2).
 */
@Getter
@Entity
@Table(name = "study")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Study extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slug;

    private String title;

    private String summary;

    private String topic;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private String leader;

    private String format;

    /** 참여자(리더 제외). 입력 순서는 sortOrder로 보존한다. */
    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<StudyParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<StudyOutput> outputs = new ArrayList<>();

    @Builder
    private Study(String slug, String title, String summary, String topic, LocalDate periodStart,
                  LocalDate periodEnd, String leader, String format) {
        updateContent(slug, title, summary, topic, periodStart, periodEnd, leader, format);
    }

    /**
     * PUT 전체 필드 교체. 등록(생성자)과 수정이 같은 메서드를 거친다.
     * 참여자 · 결과물은 각각 clear 후 add로 다시 채운다.
     */
    public void updateContent(String slug, String title, String summary, String topic, LocalDate periodStart,
                              LocalDate periodEnd, String leader, String format) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.topic = topic;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.leader = leader;
        this.format = format;
    }

    public void addParticipant(String name) {
        participants.add(StudyParticipant.of(this, name, participants.size()));
    }

    public void clearParticipants() {
        participants.clear();
    }

    public void addOutput(String label, String url) {
        outputs.add(StudyOutput.of(this, label, url, outputs.size()));
    }

    public void clearOutputs() {
        outputs.clear();
    }
}
