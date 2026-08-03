package com.retail.membership.batch.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.retail.membership.point.application.MembershipCommandService;

/**
 * userId 1건에 대해 만료 정리를 수행한다.
 *
 * <p>{@link MembershipCommandService#expirePointsForUser(String)} 로
 * API lazy expire 와 동일한 분산 락·트랜잭션·Kafka 경로를 탄다.
 */
@Component
@RequiredArgsConstructor
public class PointExpireProcessor implements ItemProcessor<String, PointExpireResult> {

    private final MembershipCommandService membershipCommandService;

    @Override
    public PointExpireResult process(String userId) {
        long expiredAmount = membershipCommandService.expirePointsForUser(userId);
        return new PointExpireResult(userId, expiredAmount);
    }
}
