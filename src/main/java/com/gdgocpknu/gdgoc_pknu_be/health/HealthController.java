package com.gdgocpknu.gdgoc_pknu_be.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 외부 cron이 주기적으로 호출해 Render 슬립과 Supabase 일시정지를 함께 예방한다.
 * 그래서 앱만 확인하지 않고 `SELECT 1`로 DB까지 실제로 건드린다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/api/health")
    public ResponseEntity<HealthResponse> health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(HealthResponse.up());
        } catch (DataAccessException e) {
            log.warn("Health check failed: database is not reachable", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(HealthResponse.dbDown());
        }
    }
}
