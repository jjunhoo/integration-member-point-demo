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

import java.time.Instant;

/**
 * <p><b>용도:</b> 통합 회원과 레거시 채널 회원번호를 연결하는 JPA 엔티티.</p>
 *
 * 채널 계정 연결.
 *
 * <p>통합 회원이 특정 비즈니스 채널(편의점/슈퍼/홈쇼핑/O4O)에 보유한 계정을 나타낸다.
 * 각 채널 레거시 시스템의 회원번호({@link #channelMemberNo})를 통합 회원에 매핑한다.
 * 통합 이전 기존 회원 백필 시에도 이 엔티티에 매핑을 적재한다.
 */
@Entity
@Getter
@Table(
        name = "channel_account",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_channel_member",
                columnNames = {"channel", "channel_member_no"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelAccount {

    /** PK. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 연결된 통합 회원 ID. */
    @Column(name = "member_id", length = 36, nullable = false)
    private String memberId;

    /** 비즈니스 채널 (CVS/SUPER/HS/O4O). */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20, nullable = false)
    private Channel channel;

    /** 채널 레거시 시스템의 회원번호(채널 내 고유). */
    @Column(name = "channel_member_no", length = 100, nullable = false)
    private String channelMemberNo;

    /** 연결 상태 (ACTIVE / UNLINKED). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ChannelAccountStatus status;

    /** 채널 계정 연결 시각. */
    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    /** 통합 회원과 채널 회원번호를 연결하는 내부 생성자. */
    private ChannelAccount(String memberId, Channel channel, String channelMemberNo) {
        this.memberId = memberId;
        this.channel = channel;
        this.channelMemberNo = channelMemberNo;
        this.status = ChannelAccountStatus.ACTIVE;
        this.linkedAt = Instant.now();
    }

    /** 채널 계정 연결 레코드를 생성한다. */
    public static ChannelAccount link(String memberId, Channel channel, String channelMemberNo) {
        return new ChannelAccount(memberId, channel, channelMemberNo);
    }

    /** 채널 계정 연결을 해제(UNLINKED) 상태로 변경한다. */
    public void unlink() {
        this.status = ChannelAccountStatus.UNLINKED;
    }

    /** 채널 계정 연결 상태. */
    public enum ChannelAccountStatus {
        /** 연결 활성. */
        ACTIVE,
        /** 연결 해제. */
        UNLINKED
    }
}
