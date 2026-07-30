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
    /** 편의점 채널. */
    CVS("편의점", "오프라인 편의점 채널"),
    /** 슈퍼마켓 채널. */
    SUPERMARKET("슈퍼", "슈퍼마켓 채널"),
    /** 홈쇼핑/커머스 채널. */
    HOME_SHOPPING("홈쇼핑", "홈쇼핑/커머스 채널"),
    /** O4O 앱 채널. */
    O4O_APP("O4O", "Online for Offline 앱 채널");

    /** 화면 표시용 브랜드명. */
    private final String brand;
    /** 채널 설명. */
    private final String description;

    /** 채널 브랜드명과 설명을 설정한다. */
    Channel(String brand, String description) {
        this.brand = brand;
        this.description = description;
    }

    /** 화면 표시용 채널 브랜드명을 반환한다. */
    public String getBrand() {
        return brand;
    }

    /** 채널 설명을 반환한다. */
    public String getDescription() {
        return description;
    }
}
