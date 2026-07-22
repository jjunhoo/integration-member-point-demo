package com.retail.membership.auth.local;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {

    Optional<LocalCredential> findByLoginId(String loginId);

    Optional<LocalCredential> findByMemberId(String memberId);

    boolean existsByLoginId(String loginId);
}
