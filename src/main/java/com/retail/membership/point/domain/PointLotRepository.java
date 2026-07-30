package com.retail.membership.point.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

/**
 * <p><b>용도:</b> PointLot FEFO 조회·만료 조회용 Spring Data JPA 리포지토리.</p>
 */
public interface PointLotRepository extends JpaRepository<PointLot, Long> {

    List<PointLot> findByUserIdOrderByExpiresAtAscEarnedAtAsc(String userId);

    List<PointLot> findByUserIdAndRemainingAmountGreaterThanAndExpiresAtAfterOrderByExpiresAtAscEarnedAtAsc(
            String userId, long remainingAmount, Instant now);

    List<PointLot> findByUserIdAndRemainingAmountGreaterThanAndExpiresAtLessThanEqual(
            String userId, long remainingAmount, Instant now);
}
