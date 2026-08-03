package com.retail.membership.point.event;

/**
 * <p><b>용도:</b> 포인트 적립/차감/만료 등 도메인 이벤트 유형 열거형.</p>
 *
 * 멤버십 도메인 이벤트 유형.
 */
public enum MembershipEventType {
    /** 포인트 적립. */
    POINT_ACCUMULATED,
    /** 포인트 차감(사용). */
    POINT_DEDUCTED,
    /** 포인트 만료(lazy expire). */
    POINT_EXPIRED,
    /** 등급 변경 (필요 시 별도 이벤트로 확장). */
    GRADE_CHANGED
}
