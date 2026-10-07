package com.gdgocpknu.gdgoc_pknu_be.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 계정 시드용 값. {@code ADMIN_LOGIN_ID} · {@code ADMIN_PASSWORD} 환경변수에서 온다.
 * 회원가입 API가 없으므로(PRD 4-3) 이 값을 코드나 마이그레이션에 커밋하지 않고, 배포 환경변수로만 관리한다.
 */
@ConfigurationProperties(prefix = "app.admin-seed")
public record AdminSeedProperties(String loginId, String password) {

    /** 둘 다 설정돼 있어야 시드를 시도한다. 하나만 있으면 설정 실수로 보고 시드를 건너뛴다. */
    public boolean isComplete() {
        return hasText(loginId) && hasText(password);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
