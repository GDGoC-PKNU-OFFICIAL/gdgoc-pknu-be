package com.gdgocpknu.gdgoc_pknu_be.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminSeedPropertiesTest {

    @Test
    void 둘_다_있어야_완전하다() {
        assertThat(new AdminSeedProperties("admin", "pw").isComplete()).isTrue();
    }

    @Test
    void null이거나_공백뿐이면_불완전하다() {
        assertThat(new AdminSeedProperties(null, "pw").isComplete()).isFalse();
        assertThat(new AdminSeedProperties("admin", null).isComplete()).isFalse();
        assertThat(new AdminSeedProperties("  ", "pw").isComplete()).isFalse();
        assertThat(new AdminSeedProperties(null, null).isComplete()).isFalse();
    }
}
