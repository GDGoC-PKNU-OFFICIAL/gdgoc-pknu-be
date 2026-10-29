package com.gdgocpknu.gdgoc_pknu_be.setting.domain;

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

/**
 * 활동 통계 한 칸(운영 기수 · 누적 멤버 · 완료 프로젝트 · 세션 · 이벤트 수). 순서는 1~4이며 `sort_order`가
 * 전역 UNIQUE라서 행을 지우고 다시 넣지 않고 제자리에서 값만 바꾼다({@link SiteSetting#replaceStats}).
 */
@Getter
@Entity
@Table(name = "site_stat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "setting_id")
    private SiteSetting setting;

    private int sortOrder;

    private String label;

    private int value;

    private String unit;

    private String suffix;

    static SiteStat of(SiteSetting setting, int sortOrder, StatValue stat) {
        SiteStat entity = new SiteStat();
        entity.setting = setting;
        entity.sortOrder = sortOrder;
        entity.apply(stat);
        return entity;
    }

    void apply(StatValue stat) {
        this.label = stat.label();
        this.value = stat.value();
        // 폼의 빈 입력("")은 "값 없음"이다. null로 저장해야 응답에서 생략된다(non_null은 빈 문자열을 남긴다).
        this.unit = blankToNull(stat.unit());
        this.suffix = blankToNull(stat.suffix());
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
