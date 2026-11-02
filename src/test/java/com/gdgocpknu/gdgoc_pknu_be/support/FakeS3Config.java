package com.gdgocpknu.gdgoc_pknu_be.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** 통합 테스트가 실제 R2에 접속하지 않게 한다. 메타 애너테이션에 포함해 테스트 간 컨텍스트 재사용을 유지한다. */
@TestConfiguration(proxyBeanMethods = false)
public class FakeS3Config {

    @Bean
    @Primary
    FakeS3Client fakeS3Client() {
        return new FakeS3Client();
    }
}
