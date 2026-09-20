package team3.gdgocpknube.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** 모든 에러 응답의 단일 모양 `{ code, message, fieldErrors? }` (API 명세서 1-7). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, List<FieldErrorItem> fieldErrors) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.message(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorItem> fieldErrors) {
        return new ErrorResponse(errorCode.name(), errorCode.message(), fieldErrors);
    }
}
