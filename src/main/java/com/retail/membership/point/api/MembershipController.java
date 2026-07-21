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

/**
 * 데모용 API.
 * <ul>
 *   <li>Command: 포인트 적립/차감 (분산 락 + 트랜잭션 + 커밋 후 이벤트 발행)</li>
 *   <li>Query: Redis 뷰에서 등급/잔액 조회 (Cache-Aside)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipCommandService commandService;
    private final MembershipViewStore viewStore;

    @PostMapping("/points/accumulate")
    public ResponseEntity<Void> accumulate(@Valid @RequestBody PointCommand command) {
        commandService.accumulatePoint(command);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/points/deduct")
    public ResponseEntity<Void> deduct(@Valid @RequestBody PointCommand command) {
        commandService.deductPoint(command);
        return ResponseEntity.accepted().build();
    }

    /** 읽기 전용 뷰 조회. (준실시간 동기화된 Redis 뷰) */
    @GetMapping("/{userId}/view")
    public ResponseEntity<MembershipView> getView(@PathVariable String userId) {
        return viewStore.find(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
