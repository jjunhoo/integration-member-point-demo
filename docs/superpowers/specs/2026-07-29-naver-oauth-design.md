# Naver OAuth (Authorization Code) Design

날짜: 2026-07-29  
상태: Approved

## 목표

네이버 로그인 인가 코드 플로우를 Vue + Spring에 연동한다.  
client_secret은 서버에만 두고, 로그인 UI에서 channel을 받지 않는다.

## 네이버 앱 등록

1. https://developers.naver.com/ 애플리케이션 등록
2. 네아로 API 사용 설정
3. Callback URL: `http://127.0.0.1:5173/auth/naver/callback`
4. Client ID/Secret → 환경변수 `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`

## API

- `GET /api/v1/auth/oauth/naver/authorize-url` → `{ authorizeUrl, state }`
- `POST /api/v1/auth/oauth/naver` body `{ code, state }` → `TokenResponse`

## 프론트

- 로그인/회원가입: 네이버 로그인 버튼
- `/auth/naver/callback`: code/state 수신 → API 호출 → 메인 이동
