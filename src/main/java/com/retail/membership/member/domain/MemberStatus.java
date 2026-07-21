package com.retail.membership.member.domain;

/** 통합 회원 상태. 채널 계정별 상태와 별개로 통합 회원 전체의 생명주기를 나타낸다. */
public enum MemberStatus {
    ACTIVE,     // 정상
    DORMANT,    // 휴면 (장기 미접속)
    WITHDRAWN   // 탈퇴
}
