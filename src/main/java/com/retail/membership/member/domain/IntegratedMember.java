package com.retail.membership.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 통합 회원 애그리거트 루트.
 *
 * <p>4대 채널을 가로지르는 "한 사람"을 나타내는 단일 식별자({@link #id})를 보유한다.
 * 각 채널 계정({@link ChannelAccount})과 소셜 로그인 아이덴티티({@link SocialAccount})는
 * 이 통합 회원 ID를 외래키로 참조한다.
 *
 * <h3>동일인 식별</h3>
 * <p>실명확인 연계정보(CI)를 기준으로 채널 간 동일인을 판별한다. CI 는 개인정보이므로
 * 저장 시 암호화가 필요하다(데모에서는 평문 컬럼으로 두되 주석으로 명시).
 */
@Entity
@Getter
@Table(name = "integrated_member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IntegratedMember {

    /** 통합 회원 고유 ID (내부 식별자, 대외 노출 X). */
    @Id
    @Column(name = "member_id", length = 36, nullable = false, updatable = false)
    private String id;

    /** 본인확인 연계정보(CI). 동일인 식별 키. ※ 운영 시 암호화 필수. */
    @Column(name = "ci", length = 200, unique = true)
    private String ci;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MemberStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private IntegratedMember(String ci, String name, String phone, String email) {
        this.id = UUID.randomUUID().toString();
        this.ci = ci;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.status = MemberStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    /** 신규 통합 회원을 생성한다. */
    public static IntegratedMember create(String ci, String name, String phone, String email) {
        return new IntegratedMember(ci, name, phone, email);
    }

    public void changeStatus(MemberStatus status) {
        this.status = status;
    }

    public void updateProfile(String name, String phone, String email) {
        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
        if (email != null) this.email = email;
    }
}
