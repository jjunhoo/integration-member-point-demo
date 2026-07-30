package com.retail.membership.point.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p><b>용도:</b> PointUsage 영속성 접근용 Spring Data JPA 리포지토리.</p>
 */
public interface PointUsageRepository extends JpaRepository<PointUsage, Long> {
}
