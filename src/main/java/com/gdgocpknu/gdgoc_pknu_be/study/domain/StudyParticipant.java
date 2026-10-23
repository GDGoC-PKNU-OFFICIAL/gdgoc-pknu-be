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

/** 스터디 참여자(이름 문자열). FK는 study 하나뿐이고 member 테이블과는 연결하지 않는다. */
@Getter
@Entity
@Table(name = "study_participant")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id")
    private Study study;

    private String name;

    private int sortOrder;

    static StudyParticipant of(Study study, String name, int sortOrder) {
        StudyParticipant participant = new StudyParticipant();
        participant.study = study;
        participant.name = name;
        participant.sortOrder = sortOrder;
        return participant;
    }
}
