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
 * <p><b>용도:</b> 차감 시 lot 별 사용 금액을 남기는 포인트 사용 이력 엔티티.</p>
 *
 * 포인트 사용(차감) 이력.
 *
 * <p>한 번의 차감 요청이 여러 lot 을 쓰면 같은 {@link #deductTxId} 로 여러 행이 생긴다.
 */
@Entity
@Getter
@Table(name = "point_usage")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(name = "amount", nullable = false)
    private long amount;

    /** 동일 차감 트랜잭션을 묶는 ID. */
    @Column(name = "deduct_tx_id", nullable = false, length = 36)
    private String deductTxId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    private PointUsage(String userId, Long lotId, long amount, String deductTxId, Instant occurredAt) {
        this.userId = userId;
        this.lotId = lotId;
        this.amount = amount;
        this.deductTxId = deductTxId;
        this.occurredAt = occurredAt;
    }

    public static PointUsage of(String userId, Long lotId, long amount, String deductTxId, Instant occurredAt) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다. amount=" + amount);
        }
        return new PointUsage(userId, lotId, amount, deductTxId, occurredAt);
    }
}
