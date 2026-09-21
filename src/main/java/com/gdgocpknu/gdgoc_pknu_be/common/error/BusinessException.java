package com.gdgocpknu.gdgoc_pknu_be.common.error;

import java.util.List;

/** 서비스 계층의 도메인 규칙 위반. GlobalExceptionHandler가 ErrorCode에 맞는 응답으로 변환한다. */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<FieldErrorItem> fieldErrors;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, List.of());
    }

    public BusinessException(ErrorCode errorCode, String field, String message) {
        this(errorCode, List.of(new FieldErrorItem(field, message)));
    }

    public BusinessException(ErrorCode errorCode, List<FieldErrorItem> fieldErrors) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<FieldErrorItem> fieldErrors() {
        return fieldErrors;
    }
}
