package com.retail.membership.member.domain;

import com.retail.membership.auth.social.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * <p><b>용도:</b> SocialAccount 영속성 접근용 Spring Data JPA 리포지토리.</p>
 */
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    /** provider와 provider 사용자 ID로 소셜 계정을 조회한다. */
    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
