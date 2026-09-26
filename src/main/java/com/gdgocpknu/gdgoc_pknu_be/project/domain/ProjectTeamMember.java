package com.gdgocpknu.gdgoc_pknu_be.project.domain;

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

/** 프로젝트 팀원(이름 · 역할 문자열). FK는 project 하나뿐이고 member 테이블과는 연결하지 않는다. */
@Getter
@Entity
@Table(name = "project_team")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectTeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    private String name;

    private String role;

    private int sortOrder;

    static ProjectTeamMember of(Project project, String name, String role, int sortOrder) {
        ProjectTeamMember member = new ProjectTeamMember();
        member.project = project;
        member.name = name;
        member.role = role;
        member.sortOrder = sortOrder;
        return member;
    }
}
