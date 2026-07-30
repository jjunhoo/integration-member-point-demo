package com.retail.membership.member.application;

import com.retail.membership.auth.social.SocialUserInfo;
import com.retail.membership.member.domain.Channel;
import com.retail.membership.member.domain.ChannelAccount;
import com.retail.membership.member.domain.ChannelAccountRepository;
import com.retail.membership.member.domain.IntegratedMember;
import com.retail.membership.member.domain.IntegratedMemberRepository;
import com.retail.membership.member.domain.SocialAccount;
import com.retail.membership.member.domain.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 통합 회원 애플리케이션 서비스.
 *
 * <p><b>용도:</b> “한 사람 = 하나의 {@link IntegratedMember}” 를 중심으로
 * 로그인 수단(소셜)과 비즈니스 채널 계정 매핑을 관리한다.
 * 인증(JWT)·포인트와는 분리된 회원 도메인 유스케이스 계층이다.
 *
 * <h3>주요 책임</h3>
 * <ul>
 *   <li>소셜 아이덴티티({@code provider + providerUserId}) → 통합 회원 조회/신규 가입</li>
 *   <li>로컬 가입용 통합 회원 생성 (자격증명은 {@code AuthService} 가 별도 저장)</li>
 *   <li>레거시 채널 계정({@link ChannelAccount}) 연결·조회</li>
 *   <li>통합 회원 단건 조회</li>
 * </ul>
 *
 * <p>채널 연결은 로그인과 독립이다. 가입 시 자동으로 채널을 만들지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final IntegratedMemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final ChannelAccountRepository channelAccountRepository;

    /**
     * 소셜 사용자 정보로 통합 회원을 조회하거나(가입되어 있으면) 신규 생성한다.
     *
     * <p>채널 계정 연결은 인증과 분리한다. 필요 시 {@link #linkChannelAccount} 로 연결한다.
     */
    @Transactional
    public IntegratedMember resolveOrRegisterBySocial(SocialUserInfo userInfo) {
        return socialAccountRepository
                .findByProviderAndProviderUserId(userInfo.provider(), userInfo.providerUserId())
                .map(social -> memberRepository.findById(social.getMemberId())
                        .orElseThrow(() -> new IllegalStateException(
                                "소셜 계정에 연결된 통합 회원이 없습니다. memberId=" + social.getMemberId())))
                .orElseGet(() -> registerNewMember(userInfo));
    }

    /** 소셜 프로필로 통합 회원과 소셜 계정 매핑을 신규 생성한다. */
    private IntegratedMember registerNewMember(SocialUserInfo userInfo) {
        // 실서비스에서는 소셜 프로필만으로 CI 를 알 수 없으므로, 본인확인 완료 후 CI 를 채운다.
        // 데모에서는 CI 미보유(null) 상태의 통합 회원으로 생성한다.
        IntegratedMember member = IntegratedMember.create(null, userInfo.name(), null, userInfo.email());
        memberRepository.save(member);

        socialAccountRepository.save(
                SocialAccount.link(member.getId(), userInfo.provider(), userInfo.providerUserId()));

        log.info("[Member] 소셜 통합 회원 생성 memberId={} provider={}",
                member.getId(), userInfo.provider());
        return member;
    }

    /**
     * 로컬 가입용 통합 회원을 생성한다.
     *
     * <p>채널 계정({@link ChannelAccount})은 인증과 분리된 통합 매핑이므로
     * 가입 시 자동 연결하지 않는다. 필요 시 {@link #linkChannelAccount} 로 연결한다.
     */
    @Transactional
    public IntegratedMember registerLocalMember(String name, String email) {
        IntegratedMember member = IntegratedMember.create(null, name, null, email);
        memberRepository.save(member);
        log.info("[Member] 로컬 통합 회원 생성 memberId={}", member.getId());
        return member;
    }

    /**
     * 해당 채널 계정이 아직 없으면 연결한다 (멱등).
     *
     * <p>이미 연결되어 있으면 아무 작업도 하지 않는다.
     */
    @Transactional
    public void ensureChannelLinked(String memberId, Channel channel, String channelMemberNo) {
        if (!channelAccountRepository.existsByMemberIdAndChannel(memberId, channel)) {
            channelAccountRepository.save(ChannelAccount.link(memberId, channel, channelMemberNo));
        }
    }

    /**
     * 기존 채널 계정을 통합 회원에 연결한다(채널 통합/백필 유스케이스).
     * 이미 다른 통합 회원에 연결된 채널 회원번호면 예외.
     */
    @Transactional
    public ChannelAccount linkChannelAccount(String memberId, Channel channel, String channelMemberNo) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 통합 회원: " + memberId));

        channelAccountRepository.findByChannelAndChannelMemberNo(channel, channelMemberNo)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "이미 다른 통합 회원에 연결된 채널 계정입니다. channel=" + channel + ", no=" + channelMemberNo);
                });

        ChannelAccount account = ChannelAccount.link(memberId, channel, channelMemberNo);
        return channelAccountRepository.save(account);
    }

    /** 통합 회원 단건 조회. 없으면 예외. */
    @Transactional(readOnly = true)
    public IntegratedMember getMember(String memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 통합 회원: " + memberId));
    }

    /** 해당 통합 회원에 연결된 채널 계정 목록 조회. */
    @Transactional(readOnly = true)
    public List<ChannelAccount> getChannelAccounts(String memberId) {
        return channelAccountRepository.findByMemberId(memberId);
    }
}
