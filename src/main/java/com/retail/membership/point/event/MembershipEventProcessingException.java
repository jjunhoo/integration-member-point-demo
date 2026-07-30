package com.retail.membership.point.event;

/**
 * <p><b>용도:</b> 이벤트 처리 실패를 알려 Kafka 재시도/DLQ 로 넘기기 위한 예외.</p>
 *
 * Consumer 의 이벤트 처리 실패를 나타내는 언체크 예외.
 * 이 예외가 리스너 밖으로 전파되면 Kafka DefaultErrorHandler 가
 * 재시도 후 DLQ 로 이관한다.
 */
public class MembershipEventProcessingException extends RuntimeException {

    public MembershipEventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
