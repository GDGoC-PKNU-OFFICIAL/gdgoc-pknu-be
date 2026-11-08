package com.gdgocpknu.gdgoc_pknu_be.storage.service;

import com.gdgocpknu.gdgoc_pknu_be.common.config.R2Properties;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.storage.UploadImagePolicy;
import com.gdgocpknu.gdgoc_pknu_be.storage.UploadTarget;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * 저장 시점의 업로드 재검증(API 명세서 2-9). 발급 때의 용량은 신고값이고 Presigned URL은 실제 크기를 제한하지 않으므로,
 * R2에 올라간 객체를 `HeadObject`로 다시 확인한다.
 *
 * <p>명세는 "실패 시 객체 삭제"이지만, 삭제는 **이 대상의 키 형식이 맞고 용량 · 형식을 어긴 객체**로 한정한다.
 * 다른 접두사의 URL을 받았을 때 지우면 멤버 사진처럼 이미 쓰이는 객체가 요청 하나로 사라질 수 있다.
 * R2 장애(404 외 오류)는 파일 문제가 아니므로 삭제하지 않고 그대로 던져 500이 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadVerifier {

    /** PresignService가 만드는 키 형식. `../` · 쿼리 문자열이 섞인 URL이 통과하지 못하게 전체 일치로 본다. */
    private static final Pattern KEY_NAME = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)");

    private final S3Client s3Client;
    private final R2Properties r2Properties;

    public void verify(String publicUrl, UploadTarget target) {
        String key = extractKey(publicUrl, target);

        HeadObjectResponse head = headOrFail(key, target);
        if (head.contentLength() > UploadImagePolicy.MAX_SIZE_BYTES) {
            deleteQuietly(key);
            throw failure(target, ErrorCode.FILE_TOO_LARGE.message());
        }
        if (UploadImagePolicy.extensionOf(head.contentType()).isEmpty()) {
            deleteQuietly(key);
            throw failure(target, ErrorCode.INVALID_FILE_TYPE.message());
        }
    }

    private String extractKey(String publicUrl, UploadTarget target) {
        String prefix = r2Properties.normalizedPublicBaseUrl() + "/" + target.keyPrefix();
        if (!publicUrl.startsWith(prefix) || !KEY_NAME.matcher(publicUrl.substring(prefix.length())).matches()) {
            throw failure(target, ErrorCode.UPLOAD_VERIFICATION_FAILED.message());
        }
        return publicUrl.substring(r2Properties.normalizedPublicBaseUrl().length() + 1);
    }

    private HeadObjectResponse headOrFail(String key, UploadTarget target) {
        try {
            return s3Client.headObject(request -> request.bucket(r2Properties.bucket()).key(key));
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw failure(target, ErrorCode.UPLOAD_VERIFICATION_FAILED.message());
            }
            throw e;
        }
    }

    /** 삭제 실패로 응답을 500으로 바꾸지 않는다. 저장은 어차피 막히고, 남은 객체는 MVP에서 수동 정리한다(명세 8-2). */
    private void deleteQuietly(String key) {
        try {
            s3Client.deleteObject(request -> request.bucket(r2Properties.bucket()).key(key));
        } catch (SdkException e) {
            log.warn("재검증에 실패한 R2 객체를 삭제하지 못했습니다. key={}", key, e);
        }
    }

    private static BusinessException failure(UploadTarget target, String message) {
        return new BusinessException(ErrorCode.UPLOAD_VERIFICATION_FAILED, target.requestField(), message);
    }
}
