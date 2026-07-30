package com.retail.membership.member.domain;

/**
 * <p><b>용도:</b> 통합 대상 4대 비즈니스 채널 열거형 (CVS/SUPER/HS/O4O).</p>
 *
 * 통합 대상 4대 비즈니스 채널.
 *
 * <p>로그인 수단(소셜 provider)과는 별개의 축이다. 한 명의 통합 회원이
 * 여러 채널의 계정을 동시에 보유할 수 있다(N:1).
 */
public enum Channel {
    CVS("편의점", "오프라인 편의점 채널"),
    SUPERMARKET("슈퍼", "슈퍼마켓 채널"),
    HOME_SHOPPING("홈쇼핑", "홈쇼핑/커머스 채널"),
    O4O_APP("O4O", "Online for Offline 앱 채널");

    private final String brand;
    private final String description;

    Channel(String brand, String description) {
        this.brand = brand;
        this.description = description;
    }

    public String getBrand() {
        return brand;
    }

    public String getDescription() {
        return description;
    }
}
