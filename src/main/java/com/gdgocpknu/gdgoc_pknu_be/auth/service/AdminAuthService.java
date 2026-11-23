package com.gdgocpknu.gdgoc_pknu_be.auth.service;

import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUser;
import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUserRepository;
import com.gdgocpknu.gdgoc_pknu_be.auth.dto.LoginRequest;
import com.gdgocpknu.gdgoc_pknu_be.auth.dto.LoginResponse;
import com.gdgocpknu.gdgoc_pknu_be.auth.jwt.JwtProvider;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 로그인(A-01). BCrypt 계산은 DB 트랜잭션 밖에서 한다 — 계산이 오래 걸리는 동안 커넥션(풀 5개)을 쥐고 있으면 몇 건만
 * 몰려도 공개 API까지 DB 커넥션을 기다린다. 조회는 리포지토리가 짧은 트랜잭션으로 끝낸다.
 */
@Service
public class AdminAuthService {

    /** 없는 아이디에도 같은 비용의 BCrypt를 돌리기 위한 자리표시 해시. 어떤 입력과도 일치하지 않는다. */
    private final String unknownUserHash;

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final KstDates kstDates;
    private final LoginAttemptLimiter loginAttemptLimiter;

    public AdminAuthService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder,
                            JwtProvider jwtProvider, KstDates kstDates, LoginAttemptLimiter loginAttemptLimiter) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.kstDates = kstDates;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.unknownUserHash = passwordEncoder.encode("unknown-user-placeholder");
    }

    /**
     * 아이디 · 비밀번호 중 뭐가 틀렸는지는 응답에서 구분하지 않는다(명세 4-1, 계정 존재 여부 노출 방지).
     * 응답 시간으로도 구분되지 않게, 없는 아이디도 BCrypt 비교를 한 번 거친다.
     */
    public LoginResponse login(LoginRequest request) {
        loginAttemptLimiter.acquire();

        Optional<AdminUser> found = adminUserRepository.findByLoginId(request.loginId());
        String hash = found.map(AdminUser::getPasswordHash).orElse(unknownUserHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (found.isEmpty() || !passwordMatches) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        loginAttemptLimiter.recordSuccess();
        JwtProvider.IssuedToken issued = jwtProvider.issue(found.get().getLoginId());
        return new LoginResponse(issued.token(), kstDates.toKst(issued.expiresAt()));
    }
}
