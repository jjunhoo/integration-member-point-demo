package com.retail.membership.member.api;

import com.retail.membership.member.domain.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p><b>용도:</b> 채널 계정 연결 API 요청 DTO.</p>
 */
public record LinkChannelRequest(
        /** 연결할 비즈니스 채널. */
        @NotNull Channel channel,
        /** 채널 레거시 회원번호. */
        @NotBlank String channelMemberNo
) {
}
