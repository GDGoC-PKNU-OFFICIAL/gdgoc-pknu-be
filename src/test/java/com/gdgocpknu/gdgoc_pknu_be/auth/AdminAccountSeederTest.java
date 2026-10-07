package com.gdgocpknu.gdgoc_pknu_be.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUser;
import com.gdgocpknu.gdgoc_pknu_be.auth.domain.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

    private AdminAccountSeeder seeder(AdminSeedProperties properties) {
        return new AdminAccountSeeder(repository, encoder, properties);
    }
}
