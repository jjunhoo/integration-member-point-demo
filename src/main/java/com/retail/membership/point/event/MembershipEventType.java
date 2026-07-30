package com.retail.membership.point.event;

/** 멤버십 도메인 이벤트 유형. */
public enum MembershipEventType {
    POINT_ACCUMULATED,   // 포인트 적립
    POINT_DEDUCTED,      // 포인트 차감(사용)
    POINT_EXPIRED,       // 포인트 만료(lazy expire)
    GRADE_CHANGED        // 등급 변경 (필요 시 별도 이벤트로 확장)
}
