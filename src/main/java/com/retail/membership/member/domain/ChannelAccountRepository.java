package com.retail.membership.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelAccountRepository extends JpaRepository<ChannelAccount, Long> {

    List<ChannelAccount> findByMemberId(String memberId);

    Optional<ChannelAccount> findByChannelAndChannelMemberNo(Channel channel, String channelMemberNo);

    boolean existsByMemberIdAndChannel(String memberId, Channel channel);
}
