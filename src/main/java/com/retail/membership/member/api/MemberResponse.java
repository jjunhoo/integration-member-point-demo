package com.retail.membership.member.api;

import com.retail.membership.member.domain.ChannelAccount;
import com.retail.membership.member.domain.IntegratedMember;

import java.util.List;

/**
 * <p><b>용도:</b> 통합 회원 정보와 연결된 채널 목록을 담는 응답 DTO.</p>
 *
 * 통합 회원 조회 응답 (연결된 채널 계정 목록 포함).
 */
public record MemberResponse(
        String memberId,
        String name,
        String email,
        String status,
        List<ChannelAccountView> channels
) {
    public record ChannelAccountView(
            String channel,
            String brand,
            String channelMemberNo,
            String status
    ) {
    }

    public static MemberResponse of(IntegratedMember member, List<ChannelAccount> channels) {
        List<ChannelAccountView> channelViews = channels.stream()
                .map(c -> new ChannelAccountView(
                        c.getChannel().name(),
                        c.getChannel().getBrand(),
                        c.getChannelMemberNo(),
                        c.getStatus().name()))
                .toList();
        return new MemberResponse(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getStatus().name(),
                channelViews);
    }
}
