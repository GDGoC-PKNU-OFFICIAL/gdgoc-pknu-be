package com.gdgocpknu.gdgoc_pknu_be.event.domain;

import com.gdgocpknu.gdgoc_pknu_be.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일정. 슬러그 · 자식 컬렉션이 없고, 같은 날짜(`date`) 안의 표시 순서를 `sortOrder`로 갖는다(API 명세서 3-3).
 * 속성 이름은 API 필드 이름(`date`, `time`)에 맞추고, 컬럼 이름만 DB 규칙(`start_date`, `time_text`)을 따른다.
 * 그래야 관리자 목록 `sort=date,desc`가 SortWhitelist를 그대로 통과한다.
 */
@Getter
@Entity
@Table(name = "event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Convert(converter = EventCategoryConverter.class)
    private EventCategory category;

    @Column(name = "start_date")
    private LocalDate date;

    private LocalDate endDate;

    @Column(name = "time_text")
    private String time;

    private String place;

    private String summary;

    /** 같은 날짜 안의 표시 순서(0부터). 요청으로 받지 않고 서버가 정한다(명세 4-4). */
    private int sortOrder;

    @Builder
    private Event(String title, EventCategory category, LocalDate date, LocalDate endDate,
                  String time, String place, String summary, int sortOrder) {
        updateContent(title, category, date, endDate, time, place, summary);
        this.sortOrder = sortOrder;
    }

    /** PUT 전체 필드 교체. 등록(생성자)과 수정이 같은 메서드를 거친다. `sortOrder`는 바꾸지 않는다. */
    public void updateContent(String title, EventCategory category, LocalDate date, LocalDate endDate,
                              String time, String place, String summary) {
        this.title = title;
        this.category = category;
        this.date = date;
        this.endDate = endDate;
        // 폼의 빈 입력("")은 "값 없음"이다. null로 저장해야 응답에서 생략된다(non_null은 빈 문자열을 남긴다).
        this.time = blankToNull(time);
        this.place = blankToNull(place);
        this.summary = blankToNull(summary);
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
