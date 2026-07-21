package com.retail.membership.member.api;

import com.retail.membership.auth.security.MemberPrincipal;
import com.retail.membership.member.application.MemberService;
import com.retail.membership.member.domain.ChannelAccount;
import com.retail.membership.member.domain.IntegratedMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통합 회원 API (인증 필요).
 *
 * <p>JWT 로 인증된 회원 본인의 통합 프로필/채널 연결을 조회·관리한다.
 */
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /** 내 통합 회원 정보 + 연결된 4대 채널 계정 조회. */
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(@AuthenticationPrincipal MemberPrincipal principal) {
        IntegratedMember member = memberService.getMember(principal.memberId());
        var channels = memberService.getChannelAccounts(principal.memberId());
        return ResponseEntity.ok(MemberResponse.of(member, channels));
    }

    /** 내 통합 회원에 채널 계정을 연결(채널 통합). */
    @PostMapping("/me/channels")
    public ResponseEntity<MemberResponse> linkChannel(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody LinkChannelRequest request) {
        ChannelAccount linked = memberService.linkChannelAccount(
                principal.memberId(), request.channel(), request.channelMemberNo());
        IntegratedMember member = memberService.getMember(principal.memberId());
        var channels = memberService.getChannelAccounts(principal.memberId());
        return ResponseEntity.ok(MemberResponse.of(member, channels));
    }
}
