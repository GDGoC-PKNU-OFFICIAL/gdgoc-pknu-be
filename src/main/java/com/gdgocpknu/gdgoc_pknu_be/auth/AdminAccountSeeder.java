package com.gdgocpknu.gdgoc_pknu_be.auth;

import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUser;
import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공용 관리자 계정 1건을 앱 기동 시 만든다(PRD 4-3: 회원가입 API 없음). 비밀번호는 깃에 두지 않고
 * {@code ADMIN_LOGIN_ID} · {@code ADMIN_PASSWORD} 환경변수로만 받아 즉시 BCrypt 해시로 바꾼 뒤 버린다.
 *
 * <p>이미 계정이 있으면 절대 건드리지 않는다 — 그렇지 않으면 재배포할 때마다 운영자가 바꾼 비밀번호가
 * 환경변수 값으로 덮어써진다. 값이 없으면 경고만 남기고 넘어간다: 로그인만 안 될 뿐, 공개 사이트는
 * 계속 떠 있어야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountSeeder implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminUserRepository.count() > 0) {
            return;
        }
        if (!properties.isComplete()) {
            log.warn("ADMIN_LOGIN_ID · ADMIN_PASSWORD가 설정되지 않아 관리자 계정을 만들지 않았습니다. "
                    + "로그인이 필요하면 두 환경변수를 설정하고 다시 기동하세요.");
            return;
        }
        adminUserRepository.save(AdminUser.create(properties.loginId(), passwordEncoder.encode(properties.password())));
        log.info("관리자 계정을 생성했습니다 (loginId={})", properties.loginId());
    }
}
