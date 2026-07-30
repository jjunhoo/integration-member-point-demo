package com.retail.membership.point.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 통합 멤버십 애그리거트 (Master DB 엔티티).
 *
 * <p>포인트 잔액/누적 및 등급을 보유한다. 실제 적립 단위는 {@link PointLot} 이며,
 * {@link #pointBalance} 는 사용 가능 lot 잔여 합의 요약이다.
 * 동시성은 상위 레이어의 분산 락으로 1차 방어하고, 낙관적 락({@link Version})으로
 * DB 레벨에서 2차 방어한다.
 */
@Entity
@Getter
@Table(name = "membership")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membership {

    /** 유저 ID(또는 멤버십 카드 고유 키). 4대 채널 공통 식별자. */
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    /** 현재 사용 가능한 포인트 잔액. */
    @Column(name = "point_balance", nullable = false)
    private long pointBalance;

    /** 등급 산정 기준이 되는 누적 적립 포인트(차감으로 감소하지 않음). */
    @Column(name = "total_accumulated_point", nullable = false)
    private long totalAccumulatedPoint;

    /** 현재 멤버십 등급. */
    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false)
    private MembershipGrade grade;

    /** 낙관적 락 버전. 락 어노테이션과 별개로 DB 최종 정합성을 보증한다. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public Membership(String userId) {
        this.userId = userId;
        this.pointBalance = 0L;
        this.totalAccumulatedPoint = 0L;
        this.grade = MembershipGrade.WELCOME;
    }

    /**
     * 포인트 적립. 잔액과 누적 포인트를 모두 증가시키고 등급을 재산정한다.
     */
    public void accumulate(long amount) {
        validatePositive(amount);
        this.pointBalance += amount;
        this.totalAccumulatedPoint += amount;
        recalculateGrade();
    }

    /**
     * 포인트 차감(사용). 잔액이 부족하면 예외를 던진다.
     * 누적 포인트는 등급 유지를 위해 감소시키지 않는다.
     */
    public void deduct(long amount) {
        validatePositive(amount);
        if (this.pointBalance < amount) {
            throw new IllegalStateException(
                    "포인트 잔액이 부족합니다. balance=" + pointBalance + ", requested=" + amount);
        }
        this.pointBalance -= amount;
    }

    private void recalculateGrade() {
        this.grade = MembershipGrade.of(this.totalAccumulatedPoint);
    }

    private void validatePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("포인트 금액은 0보다 커야 합니다. amount=" + amount);
        }
    }
}
