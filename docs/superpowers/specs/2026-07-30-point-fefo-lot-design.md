# FEFO Point Lot + Usage Design

날짜: 2026-07-30  
상태: Approved

## 목표

잔액형 포인트(`membership.point_balance`만 증감)를 **만료 포함 FEFO(lot + usage)** 로 바꾼다.

## 규칙

- 적립: `point_lot` 1건 생성. `expiresAt` 없으면 적립 시각 + 1년.
- 차감: `expires_at ASC`, 동일 시 `earned_at ASC` 로 lot `remaining` 차감.
- 한 차감이 여러 lot을 쓰면 동일 `deduct_tx_id` 로 `point_usage` 여러 행.
- `membership.point_balance` = 사용 가능 lot 잔여 합 요약.
- `total_accumulated_point` / 등급은 기존과 동일(차감·만료로 누적 적립 감소 없음).
- 만료: 적립/차감/lot 조회 시 lazy — 만료 lot remaining=0, balance 차감.

## 테이블

- `point_lot`: user_id, original_amount, remaining_amount, earned_at, expires_at
- `point_usage`: user_id, lot_id, amount, deduct_tx_id, occurred_at

## API

- `POST /api/v1/membership/points/accumulate` body `{ userId, amount, expiresAt? }`
- `POST /api/v1/membership/points/deduct` body `{ userId, amount }`
- `GET /api/v1/membership/{userId}/point-lots`
- Redis 뷰/Kafka 이벤트는 잔액 스냅샷 유지

## 범위 밖

- 차감 취소/환불 역분개
- 만료 배치 스케줄러
- lot 단위 Kafka 이벤트
