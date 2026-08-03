package com.retail.membership.batch.job;

/**
 * 유저 1명에 대한 만료 처리 결과 (Processor → Writer).
 *
 * @param userId        대상 유저
 * @param expiredAmount 이번에 만료 처리한 포인트 (0 이면 실질 변경 없음)
 */
public record PointExpireResult(String userId, long expiredAmount) {
}
