package com.retail.membership.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * <p><b>용도:</b> IntegratedMember 영속성 접근용 Spring Data JPA 리포지토리.</p>
 */
public interface IntegratedMemberRepository extends JpaRepository<IntegratedMember, String> {

    /** CI(본인확인 연계정보)로 통합 회원을 조회한다. */
    Optional<IntegratedMember> findByCi(String ci);
}
