package com.retail.membership.batch.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * 청크 단위 만료 결과를 집계·로깅한다.
 *
 * <p>실제 DB/이벤트 반영은 Processor 단계에서 이미 끝났고,
 * Writer 는 청크 단위 관측(로그/메트릭) 역할이다.
 */
@Slf4j
@Component
public class PointExpireWriter implements ItemWriter<PointExpireResult> {

    @Override
    public void write(Chunk<? extends PointExpireResult> chunk) {
        long users = 0L;
        long expiredAmount = 0L;

        for (PointExpireResult result : chunk) {
            if (result.expiredAmount() > 0) {
                users++;
                expiredAmount += result.expiredAmount();
            }
        }

        log.info("[Batch] pointExpire chunk 완료 size={} expiredUsers={} expiredAmount={}",
                chunk.size(), users, expiredAmount);
    }
}
