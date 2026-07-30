package com.retail.membership.point.domain;

/**
 * <p><b>용도:</b> 누적 포인트 기준 멤버십 등급 열거형.</p>
 *
 * 통합 멤버십 등급. 누적 포인트 기준으로 산정한다.
 * (실제 등급 정책은 채널 통합 정책 테이블로 외부화하는 것이 일반적이다.)
 */
public enum MembershipGrade {
    WELCOME(0),
    SILVER(30_000),
    GOLD(100_000),
    VIP(300_000);

    private final long threshold;

    MembershipGrade(long threshold) {
        this.threshold = threshold;
    }

    /** 누적 포인트에 해당하는 등급을 반환한다. */
    public static MembershipGrade of(long totalAccumulatedPoint) {
        MembershipGrade result = WELCOME;
        for (MembershipGrade grade : values()) {
            if (totalAccumulatedPoint >= grade.threshold) {
                result = grade;
            }
        }
        return result;
    }
}
