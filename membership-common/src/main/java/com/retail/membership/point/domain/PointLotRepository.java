package com.retail.membership.point.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * <p><b>용도:</b> PointLot FEFO 조회·만료 조회용 Spring Data JPA 리포지토리.</p>
 */
public interface PointLotRepository extends JpaRepository<PointLot, Long> {

    /**
     * 유저의 전체 lot 을 FEFO 순으로 조회한다.
     *
     * <pre>
     * WHERE user_id = ?
     * ORDER BY expires_at ASC, earned_at ASC
     * </pre>
     */
    List<PointLot> findByUserIdOrderByExpiresAtAscEarnedAtAsc(String userId);

    /**
     * 차감용 사용 가능 lot 을 FEFO 순으로 조회한다.
     * (잔여 &gt; 0 이고 만료 시각이 아직 지나지 않은 lot)
     *
     * <pre>
     * WHERE user_id = ?
     *   AND remaining_amount &gt; ?
     *   AND expires_at &gt; ?
     * ORDER BY expires_at ASC, earned_at ASC
     * </pre>
     */
    List<PointLot> findByUserIdAndRemainingAmountGreaterThanAndExpiresAtAfterOrderByExpiresAtAscEarnedAtAsc(
            String userId, long remainingAmount, Instant now);

    /**
     * lazy 만료 대상 lot 을 조회한다.
     * (잔여 &gt; 0 이고 만료 시각이 기준 시각 이하인 lot)
     *
     * <pre>
     * WHERE user_id = ?
     *   AND remaining_amount &gt; ?
     *   AND expires_at &lt;= ?
     * </pre>
     */
    List<PointLot> findByUserIdAndRemainingAmountGreaterThanAndExpiresAtLessThanEqual(
            String userId, long remainingAmount, Instant now);

    /**
     * 배치 만료 Job 용: 잔여가 남아 있고 이미 만료된 lot 을 가진 유저 ID 목록.
     *
     * <pre>
     * SELECT DISTINCT user_id
     * FROM point_lot
     * WHERE remaining_amount &gt; 0
     *   AND expires_at &lt;= :now
     * </pre>
     */
    @Query("""
            SELECT DISTINCT p.userId FROM PointLot p
            WHERE p.remainingAmount > 0
              AND p.expiresAt <= :now
            """)
    Page<String> findDistinctUserIdsWithExpiredLots(@Param("now") Instant now, Pageable pageable);
}
