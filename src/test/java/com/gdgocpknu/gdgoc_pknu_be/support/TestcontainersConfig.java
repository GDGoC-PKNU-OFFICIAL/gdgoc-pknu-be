package com.gdgocpknu.gdgoc_pknu_be.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 통합 테스트는 H2가 아니라 운영과 같은 PostgreSQL로 돈다 (jsonb · CHECK 제약 재현). docker-compose.yml과 버전을 맞춘다. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"));
    }
}
