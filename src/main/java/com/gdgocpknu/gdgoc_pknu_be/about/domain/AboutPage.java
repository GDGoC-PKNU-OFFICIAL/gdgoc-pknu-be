package com.gdgocpknu.gdgoc_pknu_be.about.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * About 페이지 내용. 사이트 설정처럼 단일 행(`id = 1`, DB CHECK)이고 PUT으로 통째로 바꾼다.
 * 구역(소개 · 활동 · 규칙 · FAQ)은 화면에서 한 묶음으로 편집 · 표시되므로 따로 테이블을 두지 않고 JSON 배열로 저장한다.
 * `created_at`이 없어 {@code BaseTimeEntity}를 쓰지 않고 `updatedAt`만 둔다.
 */
@Getter
@Entity
@Table(name = "about_page")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AboutPage {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;

    /** Who We Are 문단. 문단 안의 줄바꿈(\n)은 화면에서 그대로 줄이 바뀐다. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> identity = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<AboutActivity> activities = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<AboutRule> rules = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<AboutFaq> faq = new ArrayList<>();

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static AboutPage create(List<String> identity, List<AboutActivity> activities, List<AboutRule> rules,
                                   List<AboutFaq> faq) {
        AboutPage page = new AboutPage();
        page.id = SINGLETON_ID;
        page.update(identity, activities, rules, faq);
        return page;
    }

    /** PUT 전체 교체. */
    public void update(List<String> identity, List<AboutActivity> activities, List<AboutRule> rules,
                       List<AboutFaq> faq) {
        this.identity = new ArrayList<>(identity);
        this.activities = new ArrayList<>(activities);
        this.rules = new ArrayList<>(rules);
        this.faq = new ArrayList<>(faq);
    }

    /** 내용이 같아도 저장하면 수정 시각을 갱신한다 (JSON 칸은 값이 같으면 dirty가 아니다). */
    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
