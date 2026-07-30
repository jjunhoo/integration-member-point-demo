package com.retail.membership.auth.social;

/**
 * <p><b>용도:</b> 소셜/OAuth 인증 실패 시 던지는 런타임 예외.</p>
 *
 * 소셜 인증 실패 예외.
 */
public class SocialAuthException extends RuntimeException {

    /** 메시지만 담는 소셜 인증 실패 예외를 생성한다. */
    public SocialAuthException(String message) {
        super(message);
    }

    /** 원인 예외를 포함한 소셜 인증 실패 예외를 생성한다. */
    public SocialAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
