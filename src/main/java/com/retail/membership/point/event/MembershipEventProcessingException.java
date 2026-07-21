package com.retail.membership.point.event;

/**
 * Consumer 의 이벤트 처리 실패를 나타내는 언체크 예외.
 * 이 예외가 리스너 밖으로 전파되면 Kafka DefaultErrorHandler 가
 * 재시도 후 DLQ 로 이관한다.
 */
public class MembershipEventProcessingException extends RuntimeException {

    public MembershipEventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
