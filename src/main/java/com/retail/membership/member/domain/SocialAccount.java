package com.retail.membership.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.retail.membership.auth.social.SocialProvider;

import java.time.Instant;

/**
 * <p><b>용도:</b> 소셜 provider 아이덴티티와 통합 회원을 연결하는 JPA 엔티티.</p>
 *
 * 소셜 로그인 아이덴티티 연결.
 *
 * <p>(provider, providerUserId) 조합이 하나의 통합 회원을 가리킨다. 이 매핑을 통해
 * "카카오로 로그인한 사용자"를 통합 회원으로 해석한다. 로그인 수단은 비즈니스 채널과
 * 독립적이므로 별도 엔티티로 분리한다.
 */
@Entity
@Getter
@Table(
        name = "social_account",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_social_provider_user",
                columnNames = {"provider", "provider_user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    /** PK. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 소속 통합 회원 ID. */
    @Column(name = "member_id", length = 36, nullable = false)
    private String memberId;

    /** 소셜 provider (KAKAO/NAVER/APPLE). */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20, nullable = false)
    private SocialProvider provider;

    /** provider 가 부여한 사용자 고유 ID. */
    @Column(name = "provider_user_id", length = 100, nullable = false)
    private String providerUserId;

    /** 소셜 계정 연결 시각. */
    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    /** 통합 회원과 소셜 아이덴티티를 연결하는 내부 생성자. */
    private SocialAccount(String memberId, SocialProvider provider, String providerUserId) {
        this.memberId = memberId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.linkedAt = Instant.now();
    }

    /** 소셜 계정 연결 레코드를 생성한다. */
    public static SocialAccount link(String memberId, SocialProvider provider, String providerUserId) {
        return new SocialAccount(memberId, provider, providerUserId);
    }
}
