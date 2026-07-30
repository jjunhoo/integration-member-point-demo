package com.retail.membership.auth.social;

/**
 * <p><b>용도:</b> 소셜/OAuth 인증 실패 시 던지는 런타임 예외.</p>
 *
 * 소셜 인증 실패 예외.
 */
public class SocialAuthException extends RuntimeException {

    public SocialAuthException(String message) {
        super(message);
    }

    public SocialAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
