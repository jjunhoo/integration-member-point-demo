package com.retail.membership.auth.jwt;

/** JWT 토큰 종류. access/refresh 를 클레임으로 구분해 오용을 방지한다. */
public enum TokenType {
    ACCESS,
    REFRESH
}
