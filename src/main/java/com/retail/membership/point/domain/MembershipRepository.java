package com.retail.membership.point.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 멤버십 애그리거트 영속성 포트 (Command 측 Master DB 접근).
 */
public interface MembershipRepository extends JpaRepository<Membership, String> {
}
