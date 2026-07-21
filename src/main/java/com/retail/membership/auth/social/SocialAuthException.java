package com.retail.membership.auth.social;

/** 소셜 인증 실패 예외. */
public class SocialAuthException extends RuntimeException {

    public SocialAuthException(String message) {
        super(message);
    }

    public SocialAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
