package com.gdgocpknu.gdgoc_pknu_be.setting.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 사이트 설정. 단일 행(`id = 1`, DB CHECK)이라 목록 · 삭제가 없다. 모집 상태 배너와 활동 통계 4개를 가진다.
 * `created_at` 컬럼이 없어 {@code BaseTimeEntity}를 쓰지 않고 `updatedAt`만 직접 둔다.
 * 통계는 항상 정확히 4개(순서 1~4)이며, 수정은 행을 바꾸지 않고 값만 바꾼다.
 */
@Getter
@Entity
@Table(name = "site_setting")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteSetting {

    public static final short SINGLETON_ID = 1;
    public static final int STAT_COUNT = 4;

    @Id
    private Short id;

    private boolean recruiting;

    private String bannerText;

    private LocalDate recruitStart;

    private LocalDate recruitEnd;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "setting", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<SiteStat> stats = new ArrayList<>();

    public static SiteSetting create(boolean recruiting, String bannerText, LocalDate recruitStart,
                                     LocalDate recruitEnd, List<StatValue> stats) {
        requireStatCount(stats);
        SiteSetting setting = new SiteSetting();
        setting.id = SINGLETON_ID;
        setting.applyBanner(recruiting, bannerText, recruitStart, recruitEnd);
        for (int i = 0; i < stats.size(); i++) {
            setting.stats.add(SiteStat.of(setting, i + 1, stats.get(i)));
        }
        return setting;
    }

    /** PUT 전체 필드 교체. 통계는 같은 순서의 칸마다 값만 바꾼다(행을 지우고 다시 넣지 않는다). */
    public void update(boolean recruiting, String bannerText, LocalDate recruitStart, LocalDate recruitEnd,
                       List<StatValue> stats) {
        requireStatCount(stats);
        applyBanner(recruiting, bannerText, recruitStart, recruitEnd);
        replaceStats(stats);
    }

    /** 통계를 순서대로 덮어쓴다. `sort_order`가 전역 UNIQUE라 삭제 후 재삽입하면 INSERT가 먼저 나가 충돌한다. */
    private void replaceStats(List<StatValue> values) {
        if (stats.size() != values.size()) {
            throw new IllegalStateException("저장된 통계 " + stats.size() + "개와 요청 " + values.size() + "개가 다릅니다.");
        }
        for (int i = 0; i < values.size(); i++) {
            stats.get(i).apply(values.get(i));
        }
    }

    /** 수정 요청이면 내용이 같아도 수정 시각을 갱신한다. 통계 값만 바뀐 수정은 이 행이 dirty하지 않기 때문이다. */
    public void touch(Instant now) {
        this.updatedAt = now;
    }

    private void applyBanner(boolean recruiting, String bannerText, LocalDate recruitStart, LocalDate recruitEnd) {
        this.recruiting = recruiting;
        this.bannerText = bannerText;
        this.recruitStart = recruitStart;
        this.recruitEnd = recruitEnd;
    }

    private static void requireStatCount(List<StatValue> stats) {
        if (stats == null || stats.size() != STAT_COUNT) {
            throw new IllegalArgumentException("통계는 정확히 " + STAT_COUNT + "개여야 합니다.");
        }
    }
}
