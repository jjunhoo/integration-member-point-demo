package com.retail.membership.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * <p><b>용도:</b> ChannelAccount 영속성 접근용 Spring Data JPA 리포지토리.</p>
 */
public interface ChannelAccountRepository extends JpaRepository<ChannelAccount, Long> {

    /** 통합 회원 ID로 연결된 채널 계정 목록을 조회한다. */
    List<ChannelAccount> findByMemberId(String memberId);

    /** 채널과 채널 회원번호로 계정을 조회한다. */
    Optional<ChannelAccount> findByChannelAndChannelMemberNo(Channel channel, String channelMemberNo);

    /** 해당 통합 회원이 특정 채널에 이미 연결되어 있는지 확인한다. */
    boolean existsByMemberIdAndChannel(String memberId, Channel channel);
}
