package team3.gdgocpknube.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * V1 · V2 마이그레이션이 API 명세서 3-9의 제약을 실제 PostgreSQL에서 그대로 강제하는지 확인한다.
 * 서비스의 도메인 규칙이 뚫려도 DB가 최후 방어선이 되어야 한다.
 */
@IntegrationTest
@Transactional
class SchemaConstraintTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 명세의_테이블_10개가_모두_생성된다() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tables).contains(
                "project", "project_team", "study", "study_participant", "study_output",
                "event", "member", "site_setting", "site_stat", "admin_user");
    }

    @Test
    void 같은_슬러그는_UNIQUE가_막는다() {
        insertProject("campus-map", "2026-03-01", null);

        assertThatThrownBy(() -> insertProject("campus-map", "2026-04-01", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_project_slug");
    }

    @Test
    void 종료월이_시작월보다_빠르면_CHECK가_막는다() {
        assertThatThrownBy(() -> insertProject("a", "2026-05-01", "2026-03-01"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_project_period");
    }

    @Test
    void 기간은_월의_1일만_저장할_수_있다() {
        assertThatThrownBy(() -> insertProject("a", "2026-03-15", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_project_period_start_first_day");
    }

    @Test
    void 허용하지_않는_유형은_CHECK가_막는다() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO project (slug, title, summary, category, period_start)
                VALUES ('a', 't', 's', 'Team Project', DATE '2026-03-01')"""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_project_category");
    }

    @Test
    void 프로젝트를_지우면_팀원_행도_함께_삭제된다() {
        insertProject("campus-map", "2026-03-01", null);
        Long projectId = jdbc.queryForObject("SELECT id FROM project WHERE slug = 'campus-map'", Long.class);
        jdbc.update("INSERT INTO project_team (project_id, name, role, sort_order) VALUES (?, '홍길동', '백엔드', 0)", projectId);

        jdbc.update("DELETE FROM project WHERE id = ?", projectId);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM project_team", Long.class)).isZero();
    }

    @Test
    void 시니어에_role이_있으면_CHECK가_막는다() {
        assertThatThrownBy(() -> insertMember("senior", "core"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_member_status_role");
    }

    @Test
    void 코어멤버에_role이_없으면_CHECK가_막는다() {
        assertThatThrownBy(() -> insertMember("core", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_member_status_role");
    }

    @Test
    void 코어멤버_role_있음_시니어_role_없음은_통과한다() {
        assertThatCode(() -> {
            insertMember("core", "lead");
            insertMember("senior", null);
        }).doesNotThrowAnyException();
    }

    @Test
    void 사이트_설정은_id_1인_단일_행만_허용한다() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO site_setting (id, recruiting, banner_text) VALUES (2, true, '모집중')"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_site_setting_single_row");
    }

    @Test
    void 모집_기간은_둘_다_있거나_둘_다_없어야_한다() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO site_setting (id, recruiting, banner_text, recruit_start)
                VALUES (1, true, '모집중', DATE '2026-09-01')"""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_site_setting_recruit_period");
    }

    @Test
    void 통계는_순서_1에서_4까지만_저장할_수_있다() {
        jdbc.update("INSERT INTO site_setting (id, recruiting, banner_text) VALUES (1, true, '모집중')");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO site_stat (setting_id, sort_order, label, value) VALUES (1, 5, '기수', 1)"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_site_stat_sort_order");
    }

    @Test
    void 일정_종료일이_시작일보다_빠르면_CHECK가_막는다() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO event (title, category, start_date, end_date, sort_order)
                VALUES ('세미나', 'seminar', DATE '2026-09-20', DATE '2026-09-19', 0)"""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_event_period");
    }

    private void insertProject(String slug, String start, String end) {
        jdbc.update("""
                INSERT INTO project (slug, title, summary, category, period_start, period_end)
                VALUES (?, 't', 's', 'team-project', CAST(? AS date), CAST(? AS date))""", slug, start, end);
    }

    private void insertMember(String status, String role) {
        jdbc.update("""
                INSERT INTO member (status, role, name, department, part, bio, consent_confirmed_at)
                VALUES (?, ?, '홍길동', '컴퓨터공학과', 'backend', '소개', now())""", status, role);
    }
}
