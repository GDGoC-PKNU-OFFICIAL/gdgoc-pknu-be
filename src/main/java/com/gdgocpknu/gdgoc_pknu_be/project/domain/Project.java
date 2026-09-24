package com.gdgocpknu.gdgoc_pknu_be.project.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Convert;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.gdgocpknu.gdgoc_pknu_be.common.entity.BaseTimeEntity;

/**
 * 프로젝트. 현재/과거 status는 저장하지 않고 period_end로 조회 시 계산한다.
 * period_start · period_end는 해당 월의 1일이다 (DB CHECK).
 */
@Getter
@Entity
@Table(name = "project")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slug;

    private String title;

    private String summary;

    @Convert(converter = ProjectCategoryConverter.class)
    private ProjectCategory category;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private String thumbnailUrl;

    private String githubUrl;

    private String demoUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> description = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> techStack = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> features = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> outcomes = new ArrayList<>();

    /** 팀원은 멤버 테이블과 연결하지 않는 이름 문자열이다. 입력 순서는 sortOrder로 보존한다. */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProjectTeamMember> team = new ArrayList<>();

    @Builder
    private Project(String slug, String title, String summary, ProjectCategory category,
                    LocalDate periodStart, LocalDate periodEnd, String thumbnailUrl,
                    String githubUrl, String demoUrl, List<String> description,
                    List<String> techStack, List<String> features, List<String> outcomes) {
        updateContent(slug, title, summary, category, periodStart, periodEnd, thumbnailUrl,
                githubUrl, demoUrl, description, techStack, features, outcomes);
    }

    /**
     * PUT 전체 필드 교체(제약 2). 등록(생성자)과 수정이 같은 메서드를 거치게 해 두 경로가 벌어지지 않게 한다.
     * {@code team}은 별도로 {@link #clearTeam()} 후 {@link #addTeamMember}를 다시 호출해 채운다.
     */
    public void updateContent(String slug, String title, String summary, ProjectCategory category,
                              LocalDate periodStart, LocalDate periodEnd, String thumbnailUrl,
                              String githubUrl, String demoUrl, List<String> description,
                              List<String> techStack, List<String> features, List<String> outcomes) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.category = category;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.thumbnailUrl = thumbnailUrl;
        this.githubUrl = githubUrl;
        this.demoUrl = demoUrl;
        this.description = copyOrEmpty(description);
        this.techStack = copyOrEmpty(techStack);
        this.features = copyOrEmpty(features);
        this.outcomes = copyOrEmpty(outcomes);
    }

    /** 현재 팀원 뒤에 추가한다. sortOrder는 입력 순서(0부터)로 자동 부여된다. */
    public void addTeamMember(String name, String role) {
        team.add(ProjectTeamMember.of(this, name, role, team.size()));
    }

    /** `clear()` + `addTeamMember` 반복 = 팀원 전체 교체(5-3). orphanRemoval이 옛 행의 DELETE를 발행한다. */
    public void clearTeam() {
        team.clear();
    }

    private static List<String> copyOrEmpty(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
