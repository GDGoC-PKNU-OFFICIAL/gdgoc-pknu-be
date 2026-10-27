package com.gdgocpknu.gdgoc_pknu_be.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.Event;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventCategory;
import com.gdgocpknu.gdgoc_pknu_be.event.domain.EventRepository;
import com.gdgocpknu.gdgoc_pknu_be.event.dto.EventOrderRequest;
import com.gdgocpknu.gdgoc_pknu_be.support.IntegrationTest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
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
 * 순서 변경은 그 날짜의 행을 `PESSIMISTIC_WRITE`로 잠근 뒤 처리한다(BACK_ARCHITECTURE 5-6).
 *
 * <p>잠금이 실제로 걸리는지 순서를 강제해 확인한다:
 * <ol>
 *   <li>트랜잭션 A가 같은 `findByDate`로 그 날짜의 행을 잠그고 커밋하지 않는다</li>
 *   <li>요청 B(순서 변경)가 `SELECT ... FOR UPDATE`에서 A를 기다리는 것을 `pg_stat_activity`로 확인한다.
 *       잠금이 없으면 B는 기다리지 않고 바로 끝나 테스트가 실패한다</li>
 *   <li>A를 커밋하면 B가 이어서 처리되고, 결과는 B가 보낸 순서 그대로다</li>
 * </ol>
 * 두 트랜잭션이 실제로 커밋돼야 하므로 `@Transactional` 롤백을 쓰지 않고, 만든 행은 직접 지운다.
 */
@IntegrationTest
class EventReorderLockTest {

    /** 다른 테스트 데이터와 섞이지 않는 날짜. */
    private static final LocalDate DAY = LocalDate.of(2031, 1, 15);
    private static final Duration WAIT_LIMIT = Duration.ofSeconds(10);

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    EventRepository eventRepository;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    DataSource dataSource;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM event WHERE start_date = ?", DAY);
    }

    @Test
    void 다른_트랜잭션이_그_날짜를_잠그고_있으면_순서_변경은_기다렸다가_처리된다() throws Exception {
        Long a = save("A", 0);
        Long b = save("B", 1);
        Long c = save("C", 2);
        String body = objectMapper.writeValueAsString(new EventOrderRequest(DAY, List.of(c, a, b)));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<MvcResult> requestB = new TransactionTemplate(transactionManager).execute(txA -> {
                assertThat(eventRepository.findByDate(DAY)).hasSize(3);   // A: 행 잠금, 아직 커밋 전
                Future<MvcResult> future = executor.submit(() -> mockMvc.perform(put("/api/admin/events/order")
                                .with(user("admin"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andReturn());
                awaitSessionBlockedOnLock(future);
                return future;
            });   // 여기서 A 커밋 → B가 잠금을 얻고 처리된다

            MvcResult result = requestB.get(WAIT_LIMIT.toSeconds(), TimeUnit.SECONDS);
            assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(204);
            assertThat(List.of(sortOrderOf(c), sortOrderOf(a), sortOrderOf(b))).containsExactly(0, 1, 2);
        } finally {
            executor.shutdownNow();
        }
    }

    /** B가 A의 행 잠금을 기다리는 상태가 될 때까지 기다린다. 기다리지 않고 끝났다면 잠금이 걸리지 않은 것이다. */
    private void awaitSessionBlockedOnLock(Future<MvcResult> requestB) {
        long deadline = System.nanoTime() + WAIT_LIMIT.toNanos();
        while (blockedSessionCount() == 0) {
            if (requestB.isDone()) {
                throw new AssertionError("순서 변경이 잠금 대기 없이 끝났다 — findByDate에 잠금이 걸리지 않았다");
            }
            if (System.nanoTime() > deadline) {
                throw new AssertionError("순서 변경이 행 잠금에서 대기하지 않았다");
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    /** 트랜잭션 A와 다른 autocommit 연결로 조회해야 매번 최신 상태를 본다(ProjectSlugRaceTest 참고). */
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

    private Long save(String title, int sortOrder) {
        return eventRepository.save(Event.builder()
                .title(title).category(EventCategory.MEETING).date(DAY).sortOrder(sortOrder)
                .build()).getId();
    }

    private int sortOrderOf(Long id) {
        return jdbc.queryForObject("SELECT sort_order FROM event WHERE id = ?", Integer.class, id);
    }
}
