package com.gdgocpknu.gdgoc_pknu_be.auth.service;

import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUser;
import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUserRepository;
import com.gdgocpknu.gdgoc_pknu_be.auth.dto.LoginRequest;
import com.gdgocpknu.gdgoc_pknu_be.auth.dto.LoginResponse;
import com.gdgocpknu.gdgoc_pknu_be.auth.jwt.JwtProvider;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final KstDates kstDates;

    /** 아이디 · 비밀번호 중 뭐가 틀렸는지는 응답에서 구분하지 않는다(명세 4-1, 계정 존재 여부 노출 방지). */
    public LoginResponse login(LoginRequest request) {
        AdminUser user = adminUserRepository.findByLoginId(request.loginId())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        JwtProvider.IssuedToken issued = jwtProvider.issue(user.getLoginId());
        return new LoginResponse(issued.token(), kstDates.toKst(issued.expiresAt()));
    }
}
