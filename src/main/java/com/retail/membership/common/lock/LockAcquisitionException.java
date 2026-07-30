package com.retail.membership.common.lock;

/**
 * <p><b>용도:</b> 분산 락 획득에 최종 실패했을 때 던지는 예외 (HTTP 429 매핑).</p>
 *
 * 분산 락 획득에 최종 실패했을 때 던지는 예외.
 *
 * <p>재시도 전략까지 소진한 뒤에도 락을 얻지 못하면 발생하며,
 * 상위 계층에서 429(Too Many Requests) 등으로 매핑하기 좋다.
 */
public class LockAcquisitionException extends RuntimeException {

    /** 메시지만 담는 락 획득 실패 예외를 생성한다. */
    public LockAcquisitionException(String message) {
        super(message);
    }

    /** 원인 예외를 포함한 락 획득 실패 예외를 생성한다. */
    public LockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
