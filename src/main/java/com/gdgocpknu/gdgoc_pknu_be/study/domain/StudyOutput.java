package com.gdgocpknu.gdgoc_pknu_be.study.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 스터디 결과물 링크(label · url, 둘 다 필수 — API 명세서 4-3). */
@Getter
@Entity
@Table(name = "study_output")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyOutput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id")
    private Study study;

    private String label;

    private String url;

    private int sortOrder;

    static StudyOutput of(Study study, String label, String url, int sortOrder) {
        StudyOutput output = new StudyOutput();
        output.study = study;
        output.label = label;
        output.url = url;
        output.sortOrder = sortOrder;
        return output;
    }
}
