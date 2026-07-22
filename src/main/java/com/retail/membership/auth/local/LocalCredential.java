package com.retail.membership.auth.local;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 로컬(아이디/비밀번호) 로그인 자격 증명.
 *
 * <p>소셜 {@code SocialAccount} 와 대칭되는 인증 수단 엔티티다.
 * 비밀번호 해시는 통합 회원 애그리거트에 넣지 않고 여기에만 보관한다.
 */
@Entity
@Getter
@Table(
        name = "local_credential",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_local_login_id", columnNames = "login_id"),
                @UniqueConstraint(name = "uk_local_member_id", columnNames = "member_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", length = 36, nullable = false)
    private String memberId;

    @Column(name = "login_id", length = 30, nullable = false)
    private String loginId;

    @Column(name = "password_hash", length = 100, nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private LocalCredential(String memberId, String loginId, String passwordHash) {
        Instant now = Instant.now();
        this.memberId = memberId;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static LocalCredential create(String memberId, String loginId, String passwordHash) {
        return new LocalCredential(memberId, loginId, passwordHash);
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }
}
