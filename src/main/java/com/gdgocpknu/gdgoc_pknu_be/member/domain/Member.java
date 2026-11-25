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
 * 공개 화면 순서는 구분(코어멤버 · 시니어)마다 관리자가 정하는 `sortOrder`(1부터)다 — 역할은 없앴다(V7).
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

    /** 같은 구분 안에서의 표시 순서 (1부터, 작을수록 앞). 순서 바꾸기 · 등록 · 구분 변경 때만 바뀐다 */
    private int sortOrder;

    private String name;

    private String photoUrl;

    private String department;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> history = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> interests = new ArrayList<>();

    /** 분야 — 관리자가 직접 적는 글자 (예: "백엔드", "DevOps"). 카드에 그대로 보인다 */
    private String part;

    private String bio;

    private String githubUrl;

    /** 공개 연락 이메일 (카드의 메일 아이콘). 동의를 받은 값만 넣는다 */
    private String email;

    private Instant consentConfirmedAt;

    @Builder
    private Member(MemberStatus status, String name, String photoUrl, String department,
                   List<String> history, List<String> interests, String part, String bio,
                   String githubUrl, String email, Instant consentConfirmedAt, int sortOrder) {
        updateContent(status, name, photoUrl, department, history, interests, part, bio, githubUrl, email);
        this.consentConfirmedAt = consentConfirmedAt;
        this.sortOrder = sortOrder;
    }

    /** 표시 순서를 바꾼다 (순서 바꾸기 · 구분이 바뀌어 다른 목록 맨 뒤로 갈 때) */
    public void moveTo(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /** PUT 전체 필드 교체. 등록(생성자)과 수정이 같은 메서드를 거친다. `consentConfirmedAt`은 바꾸지 않는다. */
    public void updateContent(MemberStatus status, String name, String photoUrl, String department,
                              List<String> history, List<String> interests, String part, String bio,
                              String githubUrl, String email) {
        this.status = status;
        this.name = name;
        this.photoUrl = photoUrl;
        this.department = department;
        this.history = copyOrEmpty(history);
        this.interests = copyOrEmpty(interests);
        this.part = part;
        this.bio = bio;
        this.githubUrl = githubUrl;
        this.email = email;
    }

    private static List<String> copyOrEmpty(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
