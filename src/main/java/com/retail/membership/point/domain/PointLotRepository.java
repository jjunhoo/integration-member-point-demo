package com.retail.membership.point.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

/**
 * <p><b>용도:</b> PointLot FEFO 조회·만료 조회용 Spring Data JPA 리포지토리.</p>
 */
public interface PointLotRepository extends JpaRepository<PointLot, Long> {

    /** 유저의 전체 lot 을 FEFO 순(만료일→적립일)으로 조회한다. */
    List<PointLot> findByUserIdOrderByExpiresAtAscEarnedAtAsc(String userId);

    /** 유저의 사용 가능 lot 을 FEFO 순으로 조회한다. 잔여 &gt; 0 이고 아직 만료되지 않은 lot. */
    List<PointLot> findByUserIdAndRemainingAmountGreaterThanAndExpiresAtAfterOrderByExpiresAtAscEarnedAtAsc(
            String userId, long remainingAmount, Instant now);

    /** 유저의 만료 대상 lot 을 조회한다. 잔여 &gt; 0 이고 만료 시각이 지난 lot. */
    List<PointLot> findByUserIdAndRemainingAmountGreaterThanAndExpiresAtLessThanEqual(
            String userId, long remainingAmount, Instant now);
}
