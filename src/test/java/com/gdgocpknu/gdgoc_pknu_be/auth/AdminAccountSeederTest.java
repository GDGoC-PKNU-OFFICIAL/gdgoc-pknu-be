package com.gdgocpknu.gdgoc_pknu_be.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUser;
import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminAccountSeederTest {

    private final AdminUserRepository repository = mock(AdminUserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    @Test
    void 계정이_이미_있으면_환경변수가_있어도_건드리지_않는다() {
        given(repository.count()).willReturn(1L);
        AdminAccountSeeder seeder = seeder(new AdminSeedProperties("admin", "raw-password"));

        seeder.run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void 로그인_아이디만_있고_비밀번호가_없으면_계정을_만들지_않는다() {
        given(repository.count()).willReturn(0L);
        AdminAccountSeeder seeder = seeder(new AdminSeedProperties("admin", null));

        seeder.run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void 둘_다_비어_있으면_계정을_만들지_않는다() {
        given(repository.count()).willReturn(0L);
        AdminAccountSeeder seeder = seeder(new AdminSeedProperties("", "  "));

        seeder.run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void 계정이_없고_환경변수가_다_있으면_해시된_비밀번호로_계정을_만든다() {
        given(repository.count()).willReturn(0L);
        given(encoder.encode("raw-password")).willReturn("hashed-password");
        AdminAccountSeeder seeder = seeder(new AdminSeedProperties("admin", "raw-password"));

        seeder.run(null);

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getLoginId()).isEqualTo("admin");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
    }

    /** 가정을 고정한다: 이 인코더가 73바이트를 거부하므로 시더가 미리 걸러야 앱이 기동에 실패하지 않는다. */
    @Test
    void BCrypt는_72바이트까지만_해시한다() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

        assertThat(bcrypt.encode("a".repeat(AdminSeedProperties.MAX_PASSWORD_BYTES))).isNotBlank();
        assertThatThrownBy(() -> bcrypt.encode("a".repeat(AdminSeedProperties.MAX_PASSWORD_BYTES + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 비밀번호가_72바이트를_넘으면_인코딩에서_터지지_않고_계정을_만들지_않는다() {
        given(repository.count()).willReturn(0L);
        AdminAccountSeeder seeder = new AdminAccountSeeder(repository, new BCryptPasswordEncoder(),
                new AdminSeedProperties("admin", "a".repeat(AdminSeedProperties.MAX_PASSWORD_BYTES + 1)));

        seeder.run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void 비밀번호가_정확히_72바이트면_실제_인코더로_계정을_만든다() {
        given(repository.count()).willReturn(0L);
        AdminAccountSeeder seeder = new AdminAccountSeeder(repository, new BCryptPasswordEncoder(),
                new AdminSeedProperties("admin", "a".repeat(AdminSeedProperties.MAX_PASSWORD_BYTES)));

        seeder.run(null);

        verify(repository).save(any());
    }

    @Test
    void 길이는_글자_수가_아니라_바이트로_본다_한글_24자는_통과하고_25자는_막는다() {
        assertThat(new AdminSeedProperties("admin", "가".repeat(24)).isPasswordTooLong()).isFalse();
        assertThat(new AdminSeedProperties("admin", "가".repeat(25)).isPasswordTooLong()).isTrue();
    }

    private AdminAccountSeeder seeder(AdminSeedProperties properties) {
        return new AdminAccountSeeder(repository, encoder, properties);
    }
}
