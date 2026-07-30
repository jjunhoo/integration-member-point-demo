package com.retail.membership.member.api;

import com.retail.membership.member.domain.ChannelAccount;
import com.retail.membership.member.domain.IntegratedMember;

import java.util.List;

/**
 * <p><b>용도:</b> 통합 회원 정보와 연결된 채널 목록을 담는 응답 DTO.</p>
 */
public record MemberResponse(
        /** 통합 회원 ID. */
        String memberId,
        /** 이름. */
        String name,
        /** 이메일. */
        String email,
        /** 회원 상태. */
        String status,
        /** 연결된 채널 계정 목록. */
        List<ChannelAccountView> channels
) {
    /** 응답에 포함되는 채널 계정 한 건의 요약 뷰. */
    public record ChannelAccountView(
            /** 채널 코드 (CVS 등). */
            String channel,
            /** 채널 브랜드 표기명. */
            String brand,
            /** 채널 레거시 회원번호. */
            String channelMemberNo,
            /** 연결 상태. */
            String status
    ) {
    }

    /** 통합 회원과 채널 계정 목록으로 API 응답 DTO를 조립한다. */
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
