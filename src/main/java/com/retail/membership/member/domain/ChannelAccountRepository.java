package com.retail.membership.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * <p><b>용도:</b> ChannelAccount 영속성 접근용 Spring Data JPA 리포지토리.</p>
 */
public interface ChannelAccountRepository extends JpaRepository<ChannelAccount, Long> {

    List<ChannelAccount> findByMemberId(String memberId);

    Optional<ChannelAccount> findByChannelAndChannelMemberNo(Channel channel, String channelMemberNo);

    boolean existsByMemberIdAndChannel(String memberId, Channel channel);
}
