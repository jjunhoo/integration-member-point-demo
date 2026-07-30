package com.retail.membership.member.api;

import com.retail.membership.auth.security.MemberPrincipal;
import com.retail.membership.member.application.MemberService;
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
 * 통합 회원 API 컨트롤러 (인증 필요).
 *
 * <p>JWT 로 식별된 회원 본인의 통합 프로필과 비즈니스 채널(편의점/슈퍼 등)
 * 계정 연결을 조회·관리한다. 로그인 수단({@code social_account}/{@code local_credential})과
 * 채널 매핑({@code channel_account})은 분리되어 있다.
 *
 * <ul>
 *   <li>{@code GET  /api/v1/members/me}           : 내 통합 회원 + 채널 목록</li>
 *   <li>{@code POST /api/v1/members/me/channels}  : 채널 계정 연결</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 내 통합 회원 정보 조회.
     *
     * <p>JWT 의 memberId 로 통합 회원과 연결된 채널 계정 목록을 함께 반환한다.
     * 프론트 메인 화면의 “내 정보” 영역에 사용한다.
     */
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(@AuthenticationPrincipal MemberPrincipal principal) {
        IntegratedMember member = memberService.getMember(principal.memberId());
        var channels = memberService.getChannelAccounts(principal.memberId());
        return ResponseEntity.ok(MemberResponse.of(member, channels));
    }

    /**
     * 채널 계정 연결(채널 통합).
     *
     * <p>레거시 채널 회원번호({@code channelMemberNo})를 현재 통합 회원에 매핑한다.
     * 이미 다른 회원에 연결된 번호면 실패한다. 성공 시 갱신된 회원+채널 목록을 반환한다.
     */
    @PostMapping("/me/channels")
    public ResponseEntity<MemberResponse> linkChannel(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody LinkChannelRequest request) {
        memberService.linkChannelAccount(
                principal.memberId(), request.channel(), request.channelMemberNo());
        IntegratedMember member = memberService.getMember(principal.memberId());
        var channels = memberService.getChannelAccounts(principal.memberId());
        return ResponseEntity.ok(MemberResponse.of(member, channels));
    }
}
