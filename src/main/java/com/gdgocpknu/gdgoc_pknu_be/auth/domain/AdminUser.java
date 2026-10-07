package com.gdgocpknu.gdgoc_pknu_be.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 공용 관리자 계정. 회원가입 API가 없으므로 앱 기동 시 {@code AdminAccountSeeder}가 환경변수로 1건만 생성한다.
 * {@code admin_user}에는 {@code updated_at}이 없어(V1 참고) {@code BaseTimeEntity}를 상속하지 않는다.
 */
@Getter
@Entity
@Table(name = "admin_user")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false)
    private String loginId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private AdminUser(String loginId, String passwordHash) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
    }

    /** {@code passwordHash}는 반드시 이미 BCrypt로 해시된 값이어야 한다 — 평문을 여기 넘기지 않는다. */
    public static AdminUser create(String loginId, String passwordHash) {
        return new AdminUser(loginId, passwordHash);
    }
}
