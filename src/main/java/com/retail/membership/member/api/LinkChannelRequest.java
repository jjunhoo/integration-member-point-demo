package com.retail.membership.member.api;

import com.retail.membership.member.domain.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p><b>용도:</b> 채널 계정 연결 API 요청 DTO.</p>
 *
 * 채널 계정 연결 요청.
 */
public record LinkChannelRequest(
        @NotNull Channel channel,
        @NotBlank String channelMemberNo
) {
}
