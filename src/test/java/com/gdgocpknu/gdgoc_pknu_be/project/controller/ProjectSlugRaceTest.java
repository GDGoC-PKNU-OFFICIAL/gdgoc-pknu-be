package com.gdgocpknu.gdgoc_pknu_be.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.gdgocpknu.gdgoc_pknu_be.project.domain.Project;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectCategory;
import com.gdgocpknu.gdgoc_pknu_be.project.domain.ProjectRepository;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 같은 슬러그를 동시에 저장하면, 서비스의 사전 중복 검사를 둘 다 통과해도 DB UNIQUE가 최후에 막고 409로 변환된다
 * (BACK_ARCHITECTURE 5-4, 9장 체크리스트).
 *
 * <p>두 요청을 그냥 동시에 보내면 대부분 사전 검사에서 걸려 DB 경로를 타지 않는다. 그래서 순서를 강제한다:
 * <ol>
 *   <li>트랜잭션 A가 슬러그를 INSERT하고 커밋하지 않는다 — UNIQUE 인덱스 잠금을 쥔다</li>
 *   <li>요청 B는 커밋 안 된 행을 못 보므로 사전 검사를 통과하고, INSERT에서 A의 잠금을 기다린다</li>
 *   <li>B가 잠금 대기 중인 것을 `pg_stat_activity`로 확인한 뒤 A를 커밋한다 → B의 INSERT가 UNIQUE 위반</li>
 * </ol>
 * 두 트랜잭션이 실제로 커밋돼야 하므로 `@Transactional` 롤백을 쓰지 않고, 만든 행은 직접 지운다.
 */
@IntegrationTest
class ProjectSlugRaceTest {

    private static final String SLUG = "race-slug";
    private static final Duration WAIT_LIMIT = Duration.ofSeconds(10);

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    DataSource dataSource;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM project WHERE slug = ?", SLUG);
    }

    @Test
    void 사전_검사를_통과한_동시_저장도_DB_UNIQUE가_막고_409를_반환한다() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<MvcResult> requestB = new TransactionTemplate(transactionManager).execute(txA -> {
                projectRepository.saveAndFlush(project(SLUG));   // A: INSERT, 아직 커밋 전
                Future<MvcResult> future = executor.submit(() -> mockMvc.perform(post("/api/admin/projects")
                                .with(user("admin"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(SLUG)))
                        .andReturn());
                awaitSessionBlockedOnLock(future);
                return future;
            });   // 여기서 A 커밋 → B의 INSERT가 UNIQUE 위반으로 깨어난다

            MvcResult result = requestB.get(WAIT_LIMIT.toSeconds(), TimeUnit.SECONDS);
            String body = result.getResponse().getContentAsString();

            assertThat(result.getResponse().getStatus()).as(body).isEqualTo(409);
            assertThat((String) JsonPath.read(body, "$.code")).isEqualTo("SLUG_DUPLICATED");
            assertThat((String) JsonPath.read(body, "$.fieldErrors[0].field")).isEqualTo("slug");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM project WHERE slug = ?", Integer.class, SLUG))
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    /** B가 사전 검사를 통과해 INSERT에서 A의 잠금을 기다리는 상태가 될 때까지 기다린다. 못 가면 테스트 전제가 깨진 것이다. */
    private void awaitSessionBlockedOnLock(Future<MvcResult> requestB) {
        long deadline = System.nanoTime() + WAIT_LIMIT.toNanos();
        while (blockedSessionCount() == 0) {
            if (requestB.isDone()) {
                throw new AssertionError("두 번째 저장이 잠금 대기 없이 끝났다: " + describe(requestB));
            }
            if (System.nanoTime() > deadline) {
                throw new AssertionError("두 번째 저장이 UNIQUE 인덱스 잠금에서 대기하지 않았다 — 사전 검사에서 걸렸을 수 있다");
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    private static String describe(Future<MvcResult> requestB) {
        try {
            MvcResult result = requestB.get();
            return result.getResponse().getStatus() + " " + result.getResponse().getContentAsString();
        } catch (Exception e) {
            return e.toString();
        }
    }

    /**
     * 반드시 트랜잭션 A와 다른 연결로 조회한다. PostgreSQL은 `pg_stat_activity`를 트랜잭션 안에서 처음 읽을 때
     * 스냅숏으로 고정하므로, A의 트랜잭션에 묶인 `JdbcTemplate`으로 조회하면 첫 조회가 B의 잠금 대기보다 먼저일 때
     * 이후 B의 대기가 끝까지 보이지 않아 시간 초과로 실패한다(실측: 간헐 실패, 첫 조회를 앞당기면 매번 실패).
     * 풀에서 받은 autocommit 연결은 조회마다 새 트랜잭션이라 매번 최신 상태를 본다.
     */
    private int blockedSessionCount() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                     SELECT count(*) FROM pg_stat_activity
                     WHERE datname = current_database() AND wait_event_type = 'Lock'
                     """)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Project project(String slug) {
        return Project.builder()
                .slug(slug).title("먼저 저장").summary("요약")
                .category(ProjectCategory.OFFICIAL)
                .periodStart(LocalDate.of(2026, 3, 1))
                .build();
    }

    private static String requestBody(String slug) {
        return """
                { "title": "나중 저장", "slug": "%s", "summary": "요약", "category": "official",
                  "period": { "start": "2026.03", "end": null }, "thumbnailUrl": null,
                  "description": ["소개"], "team": [], "techStack": [], "features": [], "outcomes": [],
                  "links": { "github": null, "demo": null } }
                """.formatted(slug);
    }
}
