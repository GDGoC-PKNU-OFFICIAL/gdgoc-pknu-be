package com.gdgocpknu.gdgoc_pknu_be.common.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * R2는 S3 호환 API라 AWS SDK를 쓴다. 리전은 `auto`, 주소는 명세 5-7과 같은 경로 방식(`/{bucket}/{key}`)이다.
 * SDK 기본값인 체크섬 자동 계산은 끈다 — R2가 지원하지 않는 값이 요청에 붙을 수 있다.
 */
@Configuration
public class R2Config {

    private static final Region REGION = Region.of("auto");

    /** 재검증은 저장 트랜잭션 안에서 돌므로, R2가 느려도 커넥션을 오래 잡지 않게 시간을 제한한다. */
    private static final Duration API_CALL_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration ATTEMPT_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    S3Client s3Client(R2Properties properties) {
        return buildClient(properties, properties.endpoint());
    }

    @Bean
    S3Presigner s3Presigner(R2Properties properties) {
        return buildPresigner(properties, properties.endpoint());
    }

    static S3Client buildClient(R2Properties properties, URI endpoint) {
        return S3Client.builder()
                .region(REGION)
                .endpointOverride(endpoint)
                .credentialsProvider(credentials(properties))
                .forcePathStyle(true)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClient(UrlConnectionHttpClient.create())
                .overrideConfiguration(c -> c.apiCallTimeout(API_CALL_TIMEOUT).apiCallAttemptTimeout(ATTEMPT_TIMEOUT))
                .build();
    }

    static S3Presigner buildPresigner(R2Properties properties, URI endpoint) {
        return S3Presigner.builder()
                .region(REGION)
                .endpointOverride(endpoint)
                .credentialsProvider(credentials(properties))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private static StaticCredentialsProvider credentials(R2Properties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
    }
}
