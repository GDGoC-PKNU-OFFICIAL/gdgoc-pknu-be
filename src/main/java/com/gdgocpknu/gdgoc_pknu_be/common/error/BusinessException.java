package com.gdgocpknu.gdgoc_pknu_be.common.error;

import java.time.Duration;
import java.util.List;

/** 서비스 계층의 도메인 규칙 위반. GlobalExceptionHandler가 ErrorCode에 맞는 응답으로 변환한다. */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<FieldErrorItem> fieldErrors;
    private final Duration retryAfter;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, List.of());
    }

    /** 일정 시간 뒤에 다시 시도하면 되는 거절(예: 로그인 잠금). 응답에 `Retry-After` 헤더가 붙는다. */
    public BusinessException(ErrorCode errorCode, Duration retryAfter) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.fieldErrors = List.of();
        this.retryAfter = retryAfter;
    }

    public BusinessException(ErrorCode errorCode, String field, String message) {
        this(errorCode, List.of(new FieldErrorItem(field, message)));
    }

    public BusinessException(ErrorCode errorCode, List<FieldErrorItem> fieldErrors) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
        this.retryAfter = null;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    /** 다시 시도할 수 있기까지의 시간. 해당 없으면 `null`. */
    public Duration retryAfter() {
        return retryAfter;
    }

    public List<FieldErrorItem> fieldErrors() {
        return fieldErrors;
    }
}
