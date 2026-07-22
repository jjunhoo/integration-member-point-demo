# Local Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add local register/login/password-change APIs using a separate `LocalCredential` entity while reusing existing JWT issuance.

**Architecture:** Persist `loginId` + BCrypt hash in `local_credential`, create/link `IntegratedMember` + `ChannelAccount` on register/login, and revoke refresh on password change. Controllers stay under `/api/v1/auth` next to social flows.

**Tech Stack:** Spring Boot 3.3, Spring Security `PasswordEncoder`, JPA/H2, existing JWT + Redis refresh store.

**Spec:** `docs/superpowers/specs/2026-07-22-local-auth-design.md`

## Global Constraints

- loginId + password required; email/name optional; channel required
- Validation: loginId 4–30 `[a-zA-Z0-9_]`, password ≥ 8
- Login failure message must not reveal account existence
- Password change revokes refresh tokens
- Do not put password fields on `IntegratedMember`

---

### Task 1: LocalCredential domain

**Files:**
- Create: `src/main/java/com/retail/membership/auth/local/LocalCredential.java`
- Create: `src/main/java/com/retail/membership/auth/local/LocalCredentialRepository.java`

- [ ] Entity with unique `loginId`, unique `memberId`, `passwordHash`, timestamps
- [ ] Repository: `findByLoginId`, `findByMemberId`, `existsByLoginId`
- [ ] `changePassword(String newHash)` domain method

### Task 2: Auth DTOs + MemberService helpers

**Files:**
- Create: `RegisterRequest`, `LoginRequest`, `ChangePasswordRequest` under `auth/api`
- Modify: `MemberService` — add `registerLocal(...)` and ensure channel link helper

- [ ] Validation annotations per spec
- [ ] Member create + channel link for local (`LOCAL:{loginId}`)

### Task 3: AuthService + Controller

**Files:**
- Modify: `AuthService.java`
- Modify: `AuthController.java`

- [ ] `register`, `login`, `changePassword`
- [ ] Reuse `issueAndStore`
- [ ] Map endpoints `/register`, `/login`, `/password`

### Task 4: Docs / Postman

**Files:**
- Modify: `README.md` API table
- Modify: `postman/membership.postman_collection.json`

- [ ] Add local auth requests and collection vars `loginId`/`password`

### Task 5: Verify + commit

- [ ] `./gradlew compileJava` (or tests if present)
- [ ] Commit on feature branch and push to personal GitHub
