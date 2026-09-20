package team3.gdgocpknube.common.error;

import org.springframework.http.HttpStatus;

/** API 명세서 1-7 에러 코드 표. 코드 · HTTP 상태 · 기본 메시지를 한 곳에서 관리한다. */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "jpg, png, webp 파일만 올릴 수 있습니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "5MB 이하 파일만 올릴 수 있습니다."),
    UPLOAD_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "업로드한 이미지를 확인할 수 없습니다. 다시 올려 주세요."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해 주세요."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 항목을 찾을 수 없습니다."),
    SLUG_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 슬러그입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
