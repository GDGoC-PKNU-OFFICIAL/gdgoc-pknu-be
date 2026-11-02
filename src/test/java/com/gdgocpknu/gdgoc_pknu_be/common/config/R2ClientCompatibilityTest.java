package com.gdgocpknu.gdgoc_pknu_be.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.storage.UploadTarget;
import com.gdgocpknu.gdgoc_pknu_be.storage.dto.PresignRequest;
import com.gdgocpknu.gdgoc_pknu_be.storage.dto.PresignResponse;
import com.gdgocpknu.gdgoc_pknu_be.storage.service.PresignService;
import com.gdgocpknu.gdgoc_pknu_be.storage.service.UploadVerifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 운영과 같은 SDK 설정으로 발급 → HTTP PUT → 재검증을 끝까지 돌린다. 가짜 S3로는 서명 · 경로 · 체크섬 문제를 볼 수 없어
 * 서명을 실제로 검증하는 S3 호환 서버(Zenko CloudServer)를 쓴다. R2 자체를 검증하지는 않는다 —
 * 실제 버킷 확인 절차는 docs/r2-setup.md에 있다.
 */
@Testcontainers
class R2ClientCompatibilityTest {

    /** CloudServer 이미지에 내장된 공개 기본 계정이다. */
    private static final String ACCESS_KEY = "accessKey1";
    private static final String SECRET_KEY = "verySecretKey1";
    private static final R2Properties PROPERTIES = new R2Properties(
            "account", ACCESS_KEY, SECRET_KEY, "test-bucket", "https://test-bucket.r2.dev");
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Container
    static final GenericContainer<?> S3_SERVER = new GenericContainer<>("zenko/cloudserver@sha256:b53e57829cf7df357323e60a19c9f98d2218f1b7ccb1d7cea5761a5a227a9ee3")
            .withEnv("S3BACKEND", "mem")
            .withEnv("REMOTE_MANAGEMENT_DISABLE", "1")
            .withExposedPorts(8000)
            .waitingFor(Wait.forListeningPort());

    private static S3Client s3;
    private static S3Presigner presigner;
    private static PresignService presignService;
    private static UploadVerifier verifier;

    @BeforeAll
    static void setUp() {
        URI endpoint = URI.create("http://" + S3_SERVER.getHost() + ":" + S3_SERVER.getMappedPort(8000));
        s3 = R2Config.buildClient(PROPERTIES, endpoint);
        presigner = R2Config.buildPresigner(PROPERTIES, endpoint);
        presignService = new PresignService(presigner, PROPERTIES, new KstDates(Clock.systemUTC()));
        verifier = new UploadVerifier(s3, PROPERTIES);
        // R2의 리전 `auto`를 이 서버는 버킷 위치로 받지 않으므로 생성할 때만 위치를 지정한다.
        s3.createBucket(request -> request.bucket(PROPERTIES.bucket())
                .createBucketConfiguration(c -> c.locationConstraint("us-east-1")));
    }

    @AfterAll
    static void tearDown() {
        s3.close();
        presigner.close();
    }

    @Test
    void 정상_파일은_발급_주소로_올라가고_재검증을_통과한다() throws Exception {
        PresignResponse presigned = presign("image/webp", 1024);

        assertThat(upload(presigned, "image/webp", 1024)).isEqualTo(200);

        assertThatCode(() -> verifier.verify(presigned.publicUrl(), UploadTarget.PROJECT)).doesNotThrowAnyException();
        assertThat(s3.headObject(request -> request.bucket(PROPERTIES.bucket()).key(presigned.key())).contentType())
                .isEqualTo("image/webp");
    }

    /** 명세 5-7: 작게 신고하고 9MB를 올려도 업로드는 성공하지만, 저장 시 재검증이 막고 객체를 지운다. */
    @Test
    void 신고값을_속여_5MB를_넘게_올리면_재검증이_막고_객체를_삭제한다() throws Exception {
        PresignResponse presigned = presign("image/webp", 348_120);

        assertThat(upload(presigned, "image/webp", 9 * 1024 * 1024)).isEqualTo(200);

        assertThatThrownBy(() -> verifier.verify(presigned.publicUrl(), UploadTarget.PROJECT))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo(ErrorCode.UPLOAD_VERIFICATION_FAILED);
                    assertThat(e.fieldErrors().getFirst().field()).isEqualTo("thumbnailUrl");
                });
        assertThatThrownBy(() -> s3.headObject(request -> request.bucket(PROPERTIES.bucket()).key(presigned.key())))
                .isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    void 신고와_다른_ContentType으로는_서명이_맞지_않아_올릴_수_없다() throws Exception {
        PresignResponse presigned = presign("image/webp", 1024);

        assertThat(upload(presigned, "text/html", 1024)).isEqualTo(403);
    }

    @Test
    void 올리지_않은_객체는_재검증에_실패한다() {
        PresignResponse presigned = presign("image/png", 1024);

        assertThatThrownBy(() -> verifier.verify(presigned.publicUrl(), UploadTarget.PROJECT))
                .isInstanceOf(BusinessException.class);
    }

    private static PresignResponse presign(String contentType, long size) {
        return presignService.presign(new PresignRequest(UploadTarget.PROJECT, contentType, size));
    }

    private static int upload(PresignResponse presigned, String contentType, int size) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(presigned.uploadUrl()))
                .header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(new byte[size]))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
