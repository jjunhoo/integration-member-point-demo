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
 * <p><b>용도:</b> 로컬 loginId/password 자격증명을 담는 JPA 엔티티.</p>
 *
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

    /** PK. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 소속 통합 회원 ID (회원당 1건). */
    @Column(name = "member_id", length = 36, nullable = false)
    private String memberId;

    /** 로컬 로그인 ID (유니크). */
    @Column(name = "login_id", length = 30, nullable = false)
    private String loginId;

    /** BCrypt 등으로 해시된 비밀번호. */
    @Column(name = "password_hash", length = 100, nullable = false)
    private String passwordHash;

    /** 자격증명 생성 시각. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 비밀번호 등 마지막 변경 시각. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** 신규 로컬 자격증명 엔티티를 생성한다. */
    private LocalCredential(String memberId, String loginId, String passwordHash) {
        Instant now = Instant.now();
        this.memberId = memberId;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 회원 ID·로그인 ID·비밀번호 해시로 자격증명을 만든다. */
    public static LocalCredential create(String memberId, String loginId, String passwordHash) {
        return new LocalCredential(memberId, loginId, passwordHash);
    }

    /** 비밀번호 해시를 갱신하고 updatedAt을 기록한다. */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }
}
