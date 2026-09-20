package team3.gdgocpknube.common.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 에러 응답 `{ code, message, fieldErrors? }`을 만드는 유일한 곳 (API 명세서 1-7).
 * 컨트롤러에는 try-catch를 두지 않는다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.errorCode();
        List<FieldErrorItem> fieldErrors = e.fieldErrors().isEmpty() ? null : e.fieldErrors();
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        List<FieldErrorItem> items = e.getConstraintViolations().stream()
                .map(v -> new FieldErrorItem(lastNode(v.getPropertyPath().toString()), v.getMessage()))
                .toList();
        return validationFailed(items);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status()).body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }

    /** `@Valid @RequestBody` 검증 실패. field는 `history[1]`, `team[0].name` 같은 경로 그대로. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorItem> items = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorItem(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return body(validationFailed(items));
    }

    /** `@RequestParam` · `@PathVariable`에 붙인 제약 위반. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorItem> items = ex.getParameterValidationResults().stream()
                .flatMap(result -> {
                    String name = result.getMethodParameter().getParameterName();
                    return result.getResolvableErrors().stream()
                            .map(error -> new FieldErrorItem(name == null ? "param" : name, error.getDefaultMessage()));
                })
                .toList();
        return body(validationFailed(items));
    }

    /** 본문이 없거나 JSON 형식이 깨졌거나 타입이 맞지 않는 경우. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return body(validationFailed(List.of()));
    }

    /**
     * 그 밖의 프레임워크 예외(404 매핑 없음, 405, 415, 쿼리 파라미터 누락 · 타입 불일치 등)도 공통 모양으로 내보낸다.
     * 명세에 코드가 없는 4xx는 원래 HTTP 상태를 유지하고 VALIDATION_FAILED로 표기한다.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ErrorCode code = codeFor(statusCode);
        return ResponseEntity.status(statusCode).headers(headers).body(ErrorResponse.of(code));
    }

    private static ErrorCode codeFor(HttpStatusCode status) {
        if (status.value() == 404) {
            return ErrorCode.NOT_FOUND;
        }
        if (status.is5xxServerError()) {
            return ErrorCode.INTERNAL_ERROR;
        }
        return ErrorCode.VALIDATION_FAILED;
    }

    private static ResponseEntity<ErrorResponse> validationFailed(List<FieldErrorItem> items) {
        ErrorCode code = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, items.isEmpty() ? null : items));
    }

    private static ResponseEntity<Object> body(ResponseEntity<ErrorResponse> response) {
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    /** `create.request.title` → `title` (메서드 파라미터 검증 경로에서 마지막 노드만 사용). */
    private static String lastNode(String propertyPath) {
        int dot = propertyPath.lastIndexOf('.');
        return dot < 0 ? propertyPath : propertyPath.substring(dot + 1);
    }
}
