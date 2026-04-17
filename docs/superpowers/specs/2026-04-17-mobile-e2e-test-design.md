# Mobile App E2E Test Design

**Date:** 2026-04-17
**Branch:** feat/luminous-sanctuary-design
**Scope:** Android instrumented Compose UI E2E tests against local Docker backend

---

## Overview

Two-phase E2E test suite covering the registration → pending approval → home flow, plus a welcome greeting fix and Korean seed data for other screens.

---

## Architecture

### Test Type
Android instrumented tests (`androidTest` source set) using `compose-ui-test-junit4`. Tests launch `MainActivity`, interact with real UI fields, and assert which screen is visible. The real Ktor OkHttp client hits the Docker backend at `10.0.2.2:8080` (emulator → localhost) and Keycloak at `10.0.2.2:8091`.

### New Files
```
composeApp/src/androidTest/kotlin/com/hanmaum/dn/mobile/
  e2e/
    TestConfig.kt          # test credentials constant
    E2ETestHelper.kt       # register user via Ktor before UI tests
    Phase1_PendingTest.kt  # register → login → assert PendingScreen
    Phase2_HomeTest.kt     # login (post-approval) → assert HomeScreen
```

### build.gradle.kts additions (androidTest deps)
- `androidx.compose.ui:ui-test-junit4`
- `androidx-espresso-core` (already in version catalog)
- `androidx-testExt-junit` (already in version catalog)

---

## Test Flow

### Phase 1 — Fresh User Sees Waiting Room

**Setup (`@Before`):**
- `E2ETestHelper.registerUser(TEST_EMAIL, TEST_PASSWORD, ...)` — calls `POST /api/v1/members/register` via Ktor directly (no UI)

**Test body:**
1. Launch `MainActivity` via `createAndroidComposeRule<MainActivity>()`
2. Wait for LoginScreen to appear
3. Type `TEST_EMAIL` into email field
4. Type `TEST_PASSWORD` into password field
5. Click "Login to DN App →" button
6. Assert PendingScreen content is visible (check for pending approval text)

**Expected result:** User is routed to `PendingRoute` because `member.status != ACTIVE`

---

### Phase 2 — Approved User Reaches Home

**Precondition (manual):** Admin approves the test user in the dashboard between Phase 1 and Phase 2.

**Test body:**
1. Launch `MainActivity`
2. Login with same `TEST_EMAIL` / `TEST_PASSWORD`
3. Assert HomeScreen content is visible (e.g. bottom nav bar or home content)

**Expected result:** User is routed to `HomeRoute` because `member.status == ACTIVE`

---

### Test Credentials
Stored in `TestConfig.kt` (not committed with real values — use local override or `local.properties`):
```kotlin
object TestConfig {
    const val TEST_EMAIL     = "e2e_test@hanmaum.com"
    const val TEST_PASSWORD  = "TestPassword123!"
    const val TEST_FIRST_NAME = "E2E"
    const val TEST_LAST_NAME  = "Test"
    const val TEST_PHONE      = "01012345678"
}
```

---

## Welcome Greeting Fix

**Problem:** `LoginScreen.kt` hardcodes `"Welcome Back"` regardless of whether the user has ever logged in.

**Fix:**
- Add `hasLoggedInBefore: Boolean` key to `multiplatform-settings` (via `TokenStorageImpl` or a new `UserPreferences` wrapper)
- `LoginViewModel` reads this flag and exposes it in `LoginUiState`
- `LoginScreen` displays:
  - `"Welcome"` if `hasLoggedInBefore == false`
  - `"Welcome Back"` if `hasLoggedInBefore == true`
- After a successful login, `LoginViewModel` writes `hasLoggedInBefore = true`

**Files to change:**
- `TokenStorageImpl.kt` (or new `UserPreferencesImpl.kt`) — add `hasLoggedInBefore` read/write
- `LoginUiState.kt` — add `val isReturningUser: Boolean = false`
- `LoginViewModel.kt` — read flag on init, expose in state; write flag after successful login
- `LoginScreen.kt` — swap hardcoded `"Welcome Back"` for `if (state.isReturningUser) "Welcome Back" else "Welcome"`

---

## Korean Seed Data

Seed script: `composeApp/src/androidTest/assets/seed_korean.sql`

Run against Docker postgres before Phase 2 test (or manually):
```
docker exec -i hanmaumApp-db psql -U <user> -d <db> < seed_korean.sql
```

### Announcements (공지사항)
3–5 rows covering categories: `NOTICE`, `EVENT`, `SERMON`

Example row:
```sql
INSERT INTO announcement (public_id, title, body, category, is_pinned, start_at, created_at, updated_at)
VALUES (gen_random_uuid(), '2026년 부활절 예배 안내', '4월 20일 주일 오전 11시, 한마음교회 본당에서 부활절 연합예배가 열립니다. 모든 성도님들의 참여를 환영합니다.', 'EVENT', true, '2026-04-15', now(), now());
```

### Ministries (사역)
3–5 rows covering different teams (worship, youth, outreach, etc.)

Example row:
```sql
INSERT INTO ministry (public_id, name, short_description, long_description, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), '찬양팀', '매주 주일 예배 찬양을 인도합니다.', '찬양팀은 하나님께 드리는 예배를 음악으로 섬기는 사역팀입니다. 보컬, 기타, 드럼, 건반 파트로 구성되어 있으며 매주 토요일 연습합니다.', true, now(), now());
```

Actual SQL file to be written with 4 announcements + 4 ministries in Korean.

---

## Implementation Order

1. `build.gradle.kts` — add `androidTest` deps
2. `TestConfig.kt` + `E2ETestHelper.kt`
3. `Phase1_PendingTest.kt`
4. Welcome greeting fix (4 files)
5. `seed_korean.sql`
6. `Phase2_HomeTest.kt`
