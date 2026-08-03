package com.retail.membership.batch.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.retail.membership.point.domain.PointLotRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;

/**
 * 만료 lot 이 남은 userId 를 페이지로 읽어 내는 Reader.
 *
 * <p>처리 후 해당 유저는 조회 결과에서 빠지므로, 항상 0페이지를 다시 읽어
 * offset 증가로 인한 스킵을 피한다.
 *
 * <p>스텝마다 상태(버퍼·기준 시각)를 새로 쓰므로 {@link StepScope}.
 */
@Component
@StepScope
@RequiredArgsConstructor
public class PointExpireUserReader implements ItemReader<String> {

    static final int PAGE_SIZE = 100;

    private final PointLotRepository pointLotRepository;

    private final Instant now = Instant.now();
    private Iterator<String> buffer = Collections.emptyIterator();

    @Override
    public String read() {
        if (!buffer.hasNext()) {
            Page<String> page = pointLotRepository.findDistinctUserIdsWithExpiredLots(
                    now, PageRequest.of(0, PAGE_SIZE));
            
            if (page.isEmpty()) {
                return null;
            }

            buffer = page.iterator();
        }
        
        return buffer.next();
    }
}
