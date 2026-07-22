# Local Auth (회원가입 / 로그인 / 비밀번호 변경) Design

날짜: 2026-07-22  
상태: Approved (대화 합의)  
관련 계정/레포: personal `jjunhoo/integration-member-point-demo`

## 배경

현재 인증은 소셜(카카오/네이버/애플) + refresh/logout 만 제공한다.  
`PasswordEncoder` 빈은 있으나 일반(아이디/비밀번호) 로그인·가입 API는 없다.

## 목표

- loginId + password 기반 회원가입 / 로그인 / 비밀번호 변경
- email은 선택
- channel은 소셜과 동일하게 필수
- 기존 JWT 발급(`issueAndStore`) / refresh 저장소 재사용

## 비목표

- Spring Security Form Login / UserDetailsService 풀스택 도입
- 이메일 인증, 비밀번호 찾기, 계정 잠금
- 소셜 계정과 로컬 계정 강제 병합 UX

## 접근

**LocalCredential 별도 엔티티** (소셜 `SocialAccount`와 대칭).  
`IntegratedMember`에 password를 넣지 않는다.

## 데이터 모델

### `local_credential`

| 컬럼 | 타입/제약 | 설명 |
|------|-----------|------|
| `id` | PK | 식별자 |
| `member_id` | VARCHAR(36), unique, not null | 통합 회원 ID |
| `login_id` | VARCHAR(30), unique, not null | 로그인 ID |
| `password_hash` | VARCHAR(100), not null | BCrypt 해시 |
| `created_at` | Instant | 생성 시각 |
| `updated_at` | Instant | 비밀번호 변경 시 갱신 |

패키지 위치: `com.retail.membership.auth.local` (또는 `member.domain`과 대칭되게 auth 하위에 credential)

## API

모두 `/api/v1/auth` 하위. Security: register/login은 `permitAll`, password 변경은 인증 필요.

### POST `/api/v1/auth/register`

Request:

```json
{
  "loginId": "demo_user",
  "password": "password1",
  "channel": "CVS",
  "email": "demo@example.com",
  "name": "데모"
}
```

- `loginId`, `password`, `channel` 필수
- `email`, `name` 선택
- 성공 시 `TokenResponse` (소셜과 동일)
- 중복 loginId → 400/409

동작:
1. loginId 중복 검사
2. `IntegratedMember` 생성 (ci=null, email/name 반영)
3. `LocalCredential` 저장 (BCrypt)
4. `ChannelAccount` 자동 연결 (`LOCAL:{loginId}` 형태로 channelMemberNo 합성)
5. JWT 발급 + refresh 저장

### POST `/api/v1/auth/login`

Request:

```json
{
  "loginId": "demo_user",
  "password": "password1",
  "channel": "CVS"
}
```

- loginId 조회 → `PasswordEncoder.matches`
- 실패 시 401 (존재 여부 구분하지 않음)
- 성공 시 JWT 발급 (channel claim 포함)
- 해당 channel 계정이 없으면 자동 연결(소셜 신규와 동일한 편의)

### POST `/api/v1/auth/password`

Auth: Bearer access token

Request:

```json
{
  "currentPassword": "password1",
  "newPassword": "password2"
}
```

- 현재 비밀번호 검증 실패 → 401/400
- 성공 시 hash 갱신 + refresh revoke (재로그인 유도)
- 응답: 204 No Content

## 검증 규칙 (데모)

- `loginId`: 4~30자, `^[a-zA-Z0-9_]+$`
- `password` / `newPassword`: 8자 이상
- `email`: 있으면 `@Email`
- `channel`: `@NotNull`

## 컴포넌트 책임

| 컴포넌트 | 책임 |
|----------|------|
| `LocalCredential` + Repository | 영속화 / loginId 조회 |
| `AuthService` | register / login / changePassword 오케스트레이션 |
| `AuthController` | HTTP 매핑 + DTO 검증 |
| `PasswordEncoder` (기존) | 해시/검증 |
| `MemberService` | 회원 생성·채널 연결 헬퍼 재사용 또는 소규모 확장 |

## 보안/운영 메모

- 평문 비밀번호 로그 금지
- 로그인 실패 메시지는 동일하게 ("아이디 또는 비밀번호가 올바르지 않습니다")
- H2 `ddl-auto: create-drop` 데모 환경이므로 별도 마이그레이션 파일은 필수 아님
- 운영 jar에는 동일 API를 두되, 실제 배포 시 rate limit / 잠금 정책 추가 권장

## 문서/도구 갱신

- README 인증 API 표에 register/login/password 추가
- Postman collection에 로컬 가입·로그인·비밀번호 변경 요청 추가

## 테스트 관점 (구현 시)

- 가입 후 동일 loginId 재가입 실패
- 잘못된 비밀번호 로그인 실패
- 비밀번호 변경 후 이전 refresh로 재발급 실패
- 변경된 비밀번호로 재로그인 성공
