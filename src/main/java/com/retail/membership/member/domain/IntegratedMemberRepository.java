package com.retail.membership.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntegratedMemberRepository extends JpaRepository<IntegratedMember, String> {

    Optional<IntegratedMember> findByCi(String ci);
}
