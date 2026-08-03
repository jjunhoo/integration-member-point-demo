package com.retail.membership.point.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.retail.membership.point.application.MembershipCommandService;
import com.retail.membership.point.application.PointCommand;
import com.retail.membership.point.query.MembershipView;
import com.retail.membership.point.query.MembershipViewStore;

import java.time.Instant;
import java.util.List;

/**
 * <p><b>용도:</b> 포인트 적립/차감·Redis 뷰·lot 조회 HTTP API 진입점.</p>
 *
 * 멤버십 포인트 API 컨트롤러 (데모, {@code permitAll}).
 *
 * <p>CQRS 쓰기/읽기를 한 컨트롤러에서 제공한다.
 * <ul>
 *   <li>Command: 적립/차감 — FEFO lot + 분산 락 + 트랜잭션 커밋 후 Kafka 이벤트</li>
 *   <li>Query: Redis 잔액/등급 뷰, Master DB 의 point_lot 목록</li>
 * </ul>
 *
 * <ul>
 *   <li>{@code POST /api/v1/membership/points/accumulate} : 포인트 적립</li>
 *   <li>{@code POST /api/v1/membership/points/deduct}     : 포인트 차감</li>
 *   <li>{@code GET  /api/v1/membership/{userId}/view}     : Redis 조회 뷰</li>
 *   <li>{@code GET  /api/v1/membership/{userId}/point-lots}: lot 목록 (FEFO 확인용)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipCommandService commandService;
    private final MembershipViewStore viewStore;

    /**
     * 포인트 적립.
     *
     * <p>{@code point_lot} 을 생성하고 {@code membership} 잔액/누적/등급을 갱신한다.
     * {@code expiresAt} 이 없으면 적립 시각 + 1년. 응답은 202 Accepted (비동기 뷰 반영).
     */
    @PostMapping("/points/accumulate")
    public ResponseEntity<Void> accumulate(@Valid @RequestBody PointCommand command) {
        commandService.accumulatePoint(command);
        return ResponseEntity.accepted().build();
    }

    /**
     * 포인트 차감(사용).
     *
     * <p>만료 임박 lot 부터(FEFO) remaining 을 깎고 {@code point_usage} 에 이력을 남긴다.
     * {@code PointCommand.expiresAt} 은 무시한다. 잔액 부족 시 예외.
     */
    @PostMapping("/points/deduct")
    public ResponseEntity<Void> deduct(@Valid @RequestBody PointCommand command) {
        commandService.deductPoint(command);
        return ResponseEntity.accepted().build();
    }

    /**
     * 포인트 조회 뷰 (Redis Cache-Aside).
     *
     * <p>Redis 히트면 스냅샷을 반환하고, 미스(TTL 만료 등)면 Master DB 에서 읽어
     * Redis 에 재적재한 뒤 반환한다. 멤버십 자체가 없으면 404.
     */
    @GetMapping("/{userId}/view")
    public ResponseEntity<MembershipView> getView(@PathVariable String userId) {
        return viewStore.find(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 포인트 lot 목록 조회 (Master DB).
     *
     * <p>적립 묶음의 잔여/만료/사용가능 여부를 반환한다.
     * 조회 시 lazy 만료 정리를 수행하며, FEFO 차감 순서를 화면에서 확인할 때 사용한다.
     */
    @GetMapping("/{userId}/point-lots")
    public ResponseEntity<List<PointLotResponse>> getPointLots(@PathVariable String userId) {
        Instant now = Instant.now();
        List<PointLotResponse> lots = commandService.listLots(userId).stream()
                .map(lot -> PointLotResponse.from(lot, now))
                .toList();
        return ResponseEntity.ok(lots);
    }
}
