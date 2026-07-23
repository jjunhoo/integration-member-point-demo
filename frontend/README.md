# Membership Frontend

Vue 3 + Vite 초간단 데모 UI.

## 실행

백엔드(`./gradlew bootRun`)가 `http://localhost:8080` 에서 떠 있어야 합니다.

```bash
npm install
npm run dev
```

브라우저: http://localhost:5173

## 화면

- `/register` 회원가입
- `/login` 로그인
- `/` 메인 (내 정보 + 포인트 적립/차감)

API base URL은 `VITE_API_BASE` 로 바꿀 수 있습니다 (기본 `http://localhost:8080`).
