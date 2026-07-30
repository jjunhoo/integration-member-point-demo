package com.retail.membership.auth.local;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * <p><b>용도:</b> LocalCredential 영속성 접근용 Spring Data JPA 리포지토리.</p>
 */
public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {

    Optional<LocalCredential> findByLoginId(String loginId);

    Optional<LocalCredential> findByMemberId(String memberId);

    boolean existsByLoginId(String loginId);
}
