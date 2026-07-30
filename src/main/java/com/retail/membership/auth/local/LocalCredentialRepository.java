package com.retail.membership.auth.local;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * <p><b>용도:</b> LocalCredential 영속성 접근용 Spring Data JPA 리포지토리.</p>
 */
public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {

    /** loginId로 로컬 자격증명을 조회한다. */
    Optional<LocalCredential> findByLoginId(String loginId);

    /** memberId로 로컬 자격증명을 조회한다. */
    Optional<LocalCredential> findByMemberId(String memberId);

    /** loginId 중복 여부를 확인한다. */
    boolean existsByLoginId(String loginId);
}
