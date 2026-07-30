package com.retail.membership.auth.jwt;

/**
 * <p><b>용도:</b> JWT 가 access 인지 refresh 인지 구분하는 클레임용 열거형.</p>
 *
 * JWT 토큰 종류. access/refresh 를 클레임으로 구분해 오용을 방지한다.
 */
public enum TokenType {
    ACCESS,
    REFRESH
}
