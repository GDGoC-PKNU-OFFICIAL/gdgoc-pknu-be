package com.gdgocpknu.gdgoc_pknu_be.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUser;
import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUserRepository;
import com.gdgocpknu.gdgoc_pknu_be.auth.dto.LoginRequest;
import com.gdgocpknu.gdgoc_pknu_be.auth.jwt.JwtProvider;
import com.gdgocpknu.gdgoc_pknu_be.common.error.BusinessException;
import com.gdgocpknu.gdgoc_pknu_be.common.error.ErrorCode;
import com.gdgocpknu.gdgoc_pknu_be.common.support.KstDates;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 로그인 흐름의 순서(제한 확인 → 조회 → BCrypt 비교)와, 아이디 존재 여부가 응답 시간으로 드러나지 않는 것을 확인한다. */
class AdminAuthServiceTest {

    private static final String UNKNOWN_HASH = "hash-for-unknown-user";

    private final AdminUserRepository repository = mock(AdminUserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtProvider jwtProvider = new JwtProvider(
            new com.gdgocpknu.gdgoc_pknu_be.auth.jwt.JwtProperties("0123456789abcdef0123456789abcdef", 8),
            Clock.fixed(Instant.parse("2026-11-23T00:00:00Z"), java.time.ZoneOffset.UTC));
    private final KstDates kstDates = new KstDates(Clock.fixed(Instant.parse("2026-11-23T00:00:00Z"), java.time.ZoneOffset.UTC));
    private final LoginAttemptLimiter limiter = mock(LoginAttemptLimiter.class);

    private AdminAuthService service() {
        given(encoder.encode(any())).willReturn(UNKNOWN_HASH);
        return new AdminAuthService(repository, encoder, jwtProvider, kstDates, limiter);
    }

    @Test
    void 없는_아이디도_BCrypt_비교를_한_번_거치고_계정_존재_여부와_같은_오류를_낸다() {
        AdminAuthService service = service();
        given(repository.findByLoginId("nobody")).willReturn(Optional.empty());
        given(encoder.matches(any(), eq(UNKNOWN_HASH))).willReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("nobody", "whatever")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.LOGIN_FAILED));

        verify(encoder).matches("whatever", UNKNOWN_HASH);
    }

    @Test
    void 아이디가_있어도_비밀번호가_틀리면_같은_오류이고_성공으로_기록하지_않는다() {
        AdminAuthService service = service();
        AdminUser admin = AdminUser.create("admin", "real-hash");
        given(repository.findByLoginId("admin")).willReturn(Optional.of(admin));
        given(encoder.matches("wrong", "real-hash")).willReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "wrong")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.LOGIN_FAILED));

        verify(limiter, never()).recordSuccess();
    }

    @Test
    void 비밀번호가_맞으면_토큰을_주고_시도_횟수를_비운다() {
        AdminAuthService service = service();
        AdminUser admin = AdminUser.create("admin", "real-hash");
        given(repository.findByLoginId("admin")).willReturn(Optional.of(admin));
        given(encoder.matches("right", "real-hash")).willReturn(true);

        assertThat(service.login(new LoginRequest("admin", "right")).token()).isNotBlank();

        verify(limiter).recordSuccess();
    }

    @Test
    void 제한에_걸리면_조회도_BCrypt도_하지_않고_바로_거절한다() {
        AdminAuthService service = service();
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.LOGIN_LOCKED, java.time.Duration.ofMinutes(5)))
                .when(limiter).acquire();

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "right")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.LOGIN_LOCKED));

        verifyNoInteractions(repository);
        verify(encoder, never()).matches(any(), any());
    }

    @Test
    void 제한_확인이_조회보다_먼저다() {
        AdminAuthService service = service();
        given(repository.findByLoginId("admin")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "x"))).isInstanceOf(BusinessException.class);

        InOrder order = inOrder(limiter, repository);
        order.verify(limiter).acquire();
        order.verify(repository).findByLoginId("admin");
    }
}
