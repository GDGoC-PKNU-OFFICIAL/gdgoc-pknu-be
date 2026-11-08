package com.gdgocpknu.gdgoc_pknu_be.storage.service;

import com.gdgocpknu.gdgoc_pknu_be.common.config.R2Properties;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import com.gdgocpknu.gdgoc_pknu_be.storage.UploadImagePolicy;
import com.gdgocpknu.gdgoc_pknu_be.storage.dto.PresignRequest;
import com.gdgocpknu.gdgoc_pknu_be.storage.dto.PresignResponse;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/**
 * R2 Presigned PUT URL 발급(API 명세서 2-9). 서명에 `Content-Type`을 포함해 신고한 형식으로만 올릴 수 있다.
 * `size`는 클라이언트 신고값이라 실제 용량은 저장 시 재검증에서 다시 확인한다.
 */
@Service
@RequiredArgsConstructor
public class PresignService {

    private static final Duration EXPIRES_IN = Duration.ofMinutes(5);

    private final S3Presigner s3Presigner;
    private final R2Properties r2Properties;
    private final KstDates kstDates;

    public PresignResponse presign(PresignRequest request) {
        String extension = UploadImagePolicy.extensionOf(request.contentType())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_FILE_TYPE, "contentType", ErrorCode.INVALID_FILE_TYPE.message()));
        if (request.size() > UploadImagePolicy.MAX_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, "size", ErrorCode.FILE_TOO_LARGE.message());
        }

        String key = request.target().keyPrefix() + UUID.randomUUID() + "." + extension;
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presign -> presign
                .signatureDuration(EXPIRES_IN)
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(r2Properties.bucket())
                        .key(key)
                        .contentType(request.contentType())
                        .build()));

        return new PresignResponse(
                presigned.url().toString(),
                r2Properties.normalizedPublicBaseUrl() + "/" + key,
                key,
                kstDates.toKst(presigned.expiration()));
    }
}
