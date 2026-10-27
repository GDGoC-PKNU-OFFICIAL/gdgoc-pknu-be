package com.gdgocpknu.gdgoc_pknu_be.member.domain;

import com.gdgocpknu.gdgoc_pknu_be.common.entity.BaseTimeEntity;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 멤버(코어멤버 · 시니어). 프로젝트 · 스터디 참여자와는 연결하지 않는다(참여자는 이름 문자열).
 * 코어멤버는 역할이 필수, 시니어는 역할이 없어야 하며 DB CHECK로도 막는다(`ck_member_status_role`).
 * `consentConfirmedAt`(공개 동의 확인 시각)은 등록 때 한 번 기록하고 이후 수정에서는 바꾸지 않는다.
 */
@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = MemberStatusConverter.class)
    private MemberStatus status;

    @Convert(converter = MemberRoleConverter.class)
    private MemberRole role;

    private String name;

    private String photoUrl;

    private String department;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> history = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> interests = new ArrayList<>();

    @Convert(converter = MemberPartConverter.class)
    private MemberPart part;

    private String bio;

    private String githubUrl;

    private String homepageUrl;

    private Instant consentConfirmedAt;

    @Builder
    private Member(MemberStatus status, MemberRole role, String name, String photoUrl, String department,
                   List<String> history, List<String> interests, MemberPart part, String bio,
                   String githubUrl, String homepageUrl, Instant consentConfirmedAt) {
        updateContent(status, role, name, photoUrl, department, history, interests, part, bio,
                githubUrl, homepageUrl);
        this.consentConfirmedAt = consentConfirmedAt;
    }

    /** PUT 전체 필드 교체. 등록(생성자)과 수정이 같은 메서드를 거친다. `consentConfirmedAt`은 바꾸지 않는다. */
    public void updateContent(MemberStatus status, MemberRole role, String name, String photoUrl, String department,
                              List<String> history, List<String> interests, MemberPart part, String bio,
                              String githubUrl, String homepageUrl) {
        this.status = status;
        this.role = role;
        this.name = name;
        this.photoUrl = photoUrl;
        this.department = department;
        this.history = copyOrEmpty(history);
        this.interests = copyOrEmpty(interests);
        this.part = part;
        this.bio = bio;
        this.githubUrl = githubUrl;
        this.homepageUrl = homepageUrl;
    }

    private static List<String> copyOrEmpty(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
