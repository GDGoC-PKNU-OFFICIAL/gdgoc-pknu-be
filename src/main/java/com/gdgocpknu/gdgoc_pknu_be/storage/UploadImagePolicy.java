package com.gdgocpknu.gdgoc_pknu_be.storage;

import java.util.Map;
import java.util.Optional;

/** 업로드 이미지 규칙(API 명세서 2-9, 4-8). 발급 시 신고값 검사와 저장 시 재검증이 같은 기준을 쓴다. */
public final class UploadImagePolicy {

    /** 5MB. 이 값까지는 허용한다. */
    public static final long MAX_SIZE_BYTES = 5_242_880L;

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private UploadImagePolicy() {
    }

    public static Optional<String> extensionOf(String contentType) {
        return Optional.ofNullable(contentType).map(EXTENSION_BY_CONTENT_TYPE::get);
    }
}
