package com.gdgocpknu.gdgoc_pknu_be.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gdgocpknu.gdgoc_pknu_be.common.config.R2Properties;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.storage.UploadTarget;
import com.gdgocpknu.gdgoc_pknu_be.support.FakeS3Client;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.S3Exception;

class UploadVerifierTest {

    private static final String BASE = "https://bucket.r2.dev";
    private static final String NAME = "8f1c0000-0000-4000-8000-000000000001.webp";
    private static final String KEY = "projects/" + NAME;
    private static final String URL = BASE + "/" + KEY;

    private final FakeS3Client s3 = new FakeS3Client();
    private final UploadVerifier verifier = new UploadVerifier(
            s3, new R2Properties("account", "access", "secret", "bucket", BASE + "/"));

    @Test
    void 정확히_5MB의_허용_형식은_통과하고_객체를_지우지_않는다() {
        s3.put(KEY, 5_242_880, "image/webp");

        assertThatCode(() -> verifier.verify(URL, UploadTarget.PROJECT)).doesNotThrowAnyException();

        assertThat(s3.exists(KEY)).isTrue();
    }

    @Test
    void 용량을_1바이트라도_넘으면_객체를_지우고_필드와_용량_문구로_실패한다() {
        s3.put(KEY, 5_242_881, "image/webp");

        assertFails(URL, UploadTarget.PROJECT, "thumbnailUrl", ErrorCode.FILE_TOO_LARGE.message());

        assertThat(s3.deletedKeys()).containsExactly(KEY);
    }

    @Test
    void 허용하지_않는_ContentType이면_객체를_지우고_실패한다() {
        s3.put(KEY, 100, "text/html");

        assertFails(URL, UploadTarget.PROJECT, "thumbnailUrl", ErrorCode.INVALID_FILE_TYPE.message());

        assertThat(s3.deletedKeys()).containsExactly(KEY);
    }

    @Test
    void 객체가_없으면_실패한다() {
        assertFails(URL, UploadTarget.PROJECT, "thumbnailUrl", ErrorCode.UPLOAD_VERIFICATION_FAILED.message());

        assertThat(s3.deletedKeys()).isEmpty();
    }

    @Test
    void 멤버_대상은_photoUrl_필드로_실패한다() {
        assertFails(BASE + "/members/" + NAME, UploadTarget.MEMBER, "photoUrl",
                ErrorCode.UPLOAD_VERIFICATION_FAILED.message());
    }

    @Test
    void 다른_대상의_접두사면_객체를_확인하지도_지우지도_않는다() {
        String memberKey = "members/" + NAME;
        s3.put(memberKey, 5_242_881, "image/webp");

        assertFails(BASE + "/" + memberKey, UploadTarget.PROJECT, "thumbnailUrl",
                ErrorCode.UPLOAD_VERIFICATION_FAILED.message());

        assertThat(s3.exists(memberKey)).isTrue();
        assertThat(s3.deletedKeys()).isEmpty();
    }

    @Test
    void R2_공개_주소가_아니거나_서버가_만든_키_형식이_아니면_실패한다() {
        s3.put("members/" + NAME, 5_242_881, "image/webp");

        for (String url : new String[] {
                "https://evil.example/projects/" + NAME,
                BASE + ".evil.example/projects/" + NAME,
                BASE + "/projects/../members/" + NAME,
                BASE + "/projects/" + NAME + "?x=1",
                BASE + "/projects/" + NAME + "#x",
                BASE + "/projects/8f1c0000-0000-4000-8000-000000000001.gif",
                BASE + "/projects/a.webp"}) {
            assertFails(url, UploadTarget.PROJECT, "thumbnailUrl", ErrorCode.UPLOAD_VERIFICATION_FAILED.message());
        }

        assertThat(s3.deletedKeys()).isEmpty();
    }

    @Test
    void R2_장애는_파일_문제가_아니므로_그대로_던지고_객체를_지우지_않는다() {
        s3.put(KEY, 100, "image/webp");
        s3.failHeadWith(S3Exception.builder().statusCode(503).build());

        assertThatThrownBy(() -> verifier.verify(URL, UploadTarget.PROJECT)).isInstanceOf(S3Exception.class);

        assertThat(s3.exists(KEY)).isTrue();
    }

    @Test
    void 삭제에_실패해도_재검증_실패_응답은_유지한다() {
        s3.put(KEY, 5_242_881, "image/webp");
        s3.failDeleteWith(S3Exception.builder().statusCode(503).build());

        assertFails(URL, UploadTarget.PROJECT, "thumbnailUrl", ErrorCode.FILE_TOO_LARGE.message());
    }

    private void assertFails(String url, UploadTarget target, String field, String message) {
        assertThatThrownBy(() -> verifier.verify(url, target))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo(ErrorCode.UPLOAD_VERIFICATION_FAILED);
                    assertThat(e.fieldErrors()).singleElement().satisfies(error -> {
                        assertThat(error.field()).isEqualTo(field);
                        assertThat(error.message()).isEqualTo(message);
                    });
                });
    }
}
