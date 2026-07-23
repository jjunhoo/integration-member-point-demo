# Vue Frontend (회원가입 / 로그인 / 메인) Design

날짜: 2026-07-23  
상태: Approved (대화 합의: Approach A + 메인에 포인트 포함)

## 목표

로컬 인증·통합회원·포인트 데모를 브라우저에서 확인할 수 있는 **초간단 Vue 3 SPA**.

## 구조

- 위치: `frontend/` (Vite + Vue 3 + vue-router)
- API: `http://localhost:8080`
- 개발 서버: `http://localhost:5173`
- 백엔드: CORS 허용 (`localhost:5173`)

## 화면

| 경로 | 화면 | API |
|------|------|-----|
| `/register` | 회원가입 | `POST /api/v1/auth/register` |
| `/login` | 로그인 | `POST /api/v1/auth/login` |
| `/` | 메인 | `GET /members/me`, 포인트 적립/차감/조회, 로그아웃 |

## 동작

- JWT: `localStorage` (`accessToken`, `refreshToken`, `channel`)
- 미로그인 시 `/` → `/login`
- 포인트 `userId` = 통합 `memberId` (데모 연결)
- 적립/차감 후 짧은 대기 뒤 뷰 재조회 (Kafka 비동기 반영)

## 비목표

- 소셜 로그인 UI
- 비밀번호 변경 UI
- Spring static 번들 서빙
- 디자인 시스템/컴포넌트 라이브러리
