package com.retail.membership.common.api;

import com.retail.membership.auth.social.SocialAuthException;
import com.retail.membership.common.lock.LockAcquisitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * <p><b>용도:</b> 컨트롤러 예외를 프론트용 JSON message 응답으로 변환하는 공통 핸들러.</p>
 *
 * 데모용 공통 예외 응답 (프론트에서 message 필드를 그대로 표시).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    /** 잔액 부족 등 비즈니스 상태 오류. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(LockAcquisitionException.class)
    public ResponseEntity<Map<String, String>> handleLockAcquisition(LockAcquisitionException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(SocialAuthException.class)
    public ResponseEntity<Map<String, String>> handleSocialAuth(SocialAuthException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
