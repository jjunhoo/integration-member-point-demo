package com.retail.membership.point.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * <p><b>용도:</b> 만료일을 가진 포인트 적립 묶음(lot) 엔티티.</p>
 *
 * 포인트 적립 묶음(lot).
 *
 * <p>차감은 FEFO(만료 임박 우선)로 이 lot 들의 {@link #remainingAmount} 를 깎는다.
 */
@Entity
@Getter
@Table(name = "point_lot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLot {

    /** PK. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 적립 대상 유저(멤버십) ID. */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /** 최초 적립 금액 (이후 변경되지 않음). */
    @Column(name = "original_amount", nullable = false)
    private long originalAmount;

    /** 아직 사용·만료되지 않은 잔여 금액. */
    @Column(name = "remaining_amount", nullable = false)
    private long remainingAmount;

    /** 적립 시각. FEFO 동률 시 정렬 키로도 사용. */
    @Column(name = "earned_at", nullable = false, updatable = false)
    private Instant earnedAt;

    /** 만료 시각. 이 시각 이후에는 사용 불가(lazy expire). */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** 적립 lot 내부 생성자. 원금과 잔여 금액을 동일하게 설정한다. */
    private PointLot(String userId, long amount, Instant earnedAt, Instant expiresAt) {
        this.userId = userId;
        this.originalAmount = amount;
        this.remainingAmount = amount;
        this.earnedAt = earnedAt;
        this.expiresAt = expiresAt;
    }

    /**
     * 포인트 적립 lot 을 생성한다.
     * 금액·만료일 유효성을 검증한 뒤 새 lot 을 반환한다.
     */
    public static PointLot earn(String userId, long amount, Instant earnedAt, Instant expiresAt) {
        if (amount <= 0) {
            throw new IllegalArgumentException("적립 금액은 0보다 커야 합니다. amount=" + amount);
        }
        if (expiresAt == null || !expiresAt.isAfter(earnedAt)) {
            throw new IllegalArgumentException("만료일은 적립 시각보다 이후여야 합니다.");
        }
        return new PointLot(userId, amount, earnedAt, expiresAt);
    }

    /** 주어진 시각에 잔여가 있고 만료되지 않았으면 사용 가능하다. */
    public boolean isUsableAt(Instant now) {
        return remainingAmount > 0 && expiresAt.isAfter(now);
    }

    /** 주어진 시각에 잔여가 있으나 만료된 lot 인지 판별한다. */
    public boolean isExpiredAt(Instant now) {
        return remainingAmount > 0 && !expiresAt.isAfter(now);
    }

    /**
     * 이 lot 에서 amount 만큼 사용. 실제 차감된 금액을 반환한다.
     */
    public long consume(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다. amount=" + amount);
        }
        long burned = Math.min(remainingAmount, amount);
        this.remainingAmount -= burned;
        return burned;
    }

    /**
     * 만료로 잔여를 0으로 만든다. 만료 직전 잔여 금액을 반환한다.
     */
    public long expireRemaining() {
        long left = this.remainingAmount;
        this.remainingAmount = 0L;
        return left;
    }
}
