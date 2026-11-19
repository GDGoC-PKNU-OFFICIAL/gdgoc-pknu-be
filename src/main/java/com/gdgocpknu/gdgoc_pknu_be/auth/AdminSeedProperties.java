package com.gdgocpknu.gdgoc_pknu_be.auth;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 계정 시드용 값. {@code ADMIN_LOGIN_ID} · {@code ADMIN_PASSWORD} 환경변수에서 온다.
 * 회원가입 API가 없으므로(PRD 4-3) 이 값을 코드나 마이그레이션에 커밋하지 않고, 배포 환경변수로만 관리한다.
 */
@ConfigurationProperties(prefix = "app.admin-seed")
public record AdminSeedProperties(String loginId, String password) {

    /** BCrypt가 받는 비밀번호의 최대 길이. 이보다 길면 `BCryptPasswordEncoder.encode`가 예외를 던진다(글자 수가 아니라 바이트). */
    public static final int MAX_PASSWORD_BYTES = 72;

    /** 둘 다 설정돼 있어야 시드를 시도한다. 하나만 있으면 설정 실수로 보고 시드를 건너뛴다. */
    public boolean isComplete() {
        return hasText(loginId) && hasText(password);
    }

    /** UTF-8 바이트 기준이다. 한글은 글자당 3바이트라 24자만 넘어도 해당된다. */
    public boolean isPasswordTooLong() {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
