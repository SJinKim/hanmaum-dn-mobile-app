# Profile Hub + Settings + Personal-Info Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the profile tab's info-card list + separate edit mode with the approved three-screen structure: hub tab (identity + menu, no personal data), a new Settings screen, and a Personal-Info screen where view = edit (prefilled form, locked admin-managed rows, dirty-only Save, birthdate datepicker).

**Architecture:** One small server PR extends the `/members/me` contract (`division`, `birthDate` read; `birthDate` write). The mobile side is null-tolerant so it works against an un-upgraded backend (division/birthdate rows simply hide). Mobile changes stay inside the existing `profile`/`member` slices plus `Routes.kt`/`App.kt`; the registration `BirthdayField` is extracted to a shared component and reused.

**Spec:** `docs/superpowers/specs/2026-07-09-profile-hub-settings-redesign.md` (defaults confirmed by user 2026-07-09: birthdate editable, photo URL field stays, logout on hub, dirty-only Save, null division/group hidden on hub / "—" on Personal-Info).

**Tech Stack:** Kotlin Multiplatform + Compose Multiplatform 1.10 (Material3), Ktor 3 + kotlinx-serialization, Koin, type-safe navigation-compose; server is Spring Boot Kotlin (Jackson camelCase, `LocalDate` ISO `yyyy-MM-dd`).

## Global Constraints

- **Prerequisite:** PR #83 (`fix/profile-edit-house-number`) must be merged into `develop` before the mobile branch is cut — this plan builds on its DTO/ViewModel/test changes (including the `houseNumber` parameter and `editHouseNumber` state).
- Mobile: never work on `main`/`develop`; branch `feature/profile-hub-redesign` from fresh `develop`. Server: feature branch from its default branch, pattern `<type>/HDN-<id>-<slug>` (see server `CLAUDE.md`); never push to `main`.
- Mobile unit gate: `./gradlew :composeApp:testDevDebugUnitTest` (never bare task names). iOS gate: `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test`.
- The literal word `TODO` must not appear anywhere in `composeApp/src` (CI greps and fails). Do not write it, even in comments.
- Wire names are **camelCase** (`division`, `birthDate`) — backend is default-Jackson; a snake_case key is silently dropped (see `tasks/lessons.md`). Birthdate wire format is the ISO string `"YYYY-MM-DD"`; the UI display format is `"yyyy.MM.dd"`; conversion is `display.replace('.', '-').takeIf { it.length == 10 }` (registration precedent, `RegisterViewModel.kt:197`).
- Mobile JSON config keeps `encodeDefaults=false` semantics: request fields left `null` are omitted; backend PATCH treats missing as "keep old value". Do not add `encodeDefaults`/`explicitNulls`.
- Mobile test names in backticks: letters/digits/spaces ONLY (Kotlin/Native compiles commonTest). Server tests are JVM-only — its existing backtick style is fine there.
- No mocking library on mobile — hand-written fakes only. Server tests use its existing Mockito setup.
- All user-facing mobile strings via `LocalStrings`: every new key = `AppStrings` interface + `KoStrings` + `EnStrings` + `DeStrings` (4 edits).
- UI: only theme tokens (`MaterialTheme.colorScheme`/`typography`/`shapes`); no 1px dividers — sections separate by surface shift; every animation `spring()`; press feedback scale 0.97 + spring; detail screens get chevron-left back (44dp target) AND system swipe-back; tab screens end with `paddingBottom = 80.dp` for the pill nav; light AND dark both verified.
- Commits: `<type>(<scope>): <imperative ≤72 chars>`, body says WHY, **no AI/Co-Authored-By trailers** (overrides any harness default).

---

### Task 0: Branch setup (both repos)

**Files:** none

- [ ] **Step 1: Confirm PR #83 is merged, then cut the mobile branch**

```bash
cd /Users/seungjinkim/Documents/Private_Projects/hanmaum-dn-mobile-app
gh pr view 83 --json state -q .state   # must print MERGED — if not, STOP and ask the user
git checkout develop && git pull --ff-only && git checkout -b feature/profile-hub-redesign
```

- [ ] **Step 2: Cut the server branch**

```bash
cd /Users/seungjinkim/Documents/Private_Projects/hanmaum-dn-server
git status --short --branch   # tree must be clean; if dirty with foreign work, STOP and ask
git checkout main && git pull --ff-only && git checkout -b feat/HDN-0-member-me-division-birthdate
```

(If the server's integration branch is not `main`, use what `gh repo view --json defaultBranchRef` reports.)

---

### Task 1: SERVER — expose `division` + `birthDate`, accept `birthDate` in PATCH

**Repo:** `/Users/seungjinkim/Documents/Private_Projects/hanmaum-dn-server` (read its `CLAUDE.md` first; all paths below relative to it)

**Files:**
- Modify: `src/main/kotlin/com/hanmaum/dn/app/features/members/api/v1/dto/MemberDtos.kt` (MemberResponse ~106-120, UpdateMyProfileRequest ~206-215)
- Modify: `src/main/kotlin/com/hanmaum/dn/app/features/members/api/MemberMappers.kt` (`Member.toResponse()` ~184-199)
- Modify: `src/main/kotlin/com/hanmaum/dn/app/features/members/service/MemberService.kt` (`updateMyProfile` ~265-279)
- Test: `src/test/kotlin/com/hanmaum/dn/app/features/members/api/MemberMappersTest.kt`
- Test: `src/test/kotlin/com/hanmaum/dn/app/features/members/service/MemberServiceTest.kt`

**Interfaces:**
- Consumes: `Member.birthDate: LocalDate?` (`domain/Member.kt:58`), `ChurchGroup.division: String?` (`features/groups/domain/ChurchGroup.kt:13`), `Member.group: ChurchGroup?`.
- Produces (the wire contract Task 2 mirrors): `MemberResponse` gains `division: String?` and `birthDate: LocalDate?` (serialized `"yyyy-MM-dd"`); `UpdateMyProfileRequest` gains `birthDate: LocalDate? = null` with null=keep semantics.

- [ ] **Step 1: Write the failing tests**

In `MemberMappersTest.kt`, after the existing `toResponse includes group name` test, add (fixture `memberWithId` and `ChurchGroup` import already exist in this file; add `import java.time.LocalDate` if not present):

```kotlin
    @Test
    fun `toResponse maps division and birthDate`() {
        val member = memberWithId(7L)
        member.group = ChurchGroup(name = "다니엘조", division = "2교구")
        member.birthDate = LocalDate.of(1992, 12, 7)

        val response = member.toResponse()

        assertEquals("2교구", response.division)
        assertEquals(LocalDate.of(1992, 12, 7), response.birthDate)
    }

    @Test
    fun `toResponse maps missing group division and birthDate to null`() {
        val member = memberWithId(8L)
        val response = member.toResponse()
        assertNull(response.division)
        assertNull(response.birthDate)
    }
```

In `MemberServiceTest.kt`, after the existing `updateMyProfile preserves address fields omitted from patch` test (~line 603), add (same fixture/mock pattern as that test; add `import java.time.LocalDate` if absent):

```kotlin
    @Test
    fun `updateMyProfile updates birthDate and preserves it when omitted`() {
        val keycloakSub = UUID.randomUUID().toString()
        val member = memberWithId(1L)
        member.keycloakId = keycloakSub
        `when`(memberRepository.findByKeycloakIdAndDeletedAtIsNull(keycloakSub)).thenReturn(member)
        `when`(memberRepository.save(member)).thenReturn(member)

        val updated =
            memberService.updateMyProfile(
                keycloakSubject = keycloakSub,
                email = null,
                request = UpdateMyProfileRequest(birthDate = LocalDate.of(1992, 12, 7)),
            )
        assertEquals(LocalDate.of(1992, 12, 7), updated.birthDate)

        val preserved =
            memberService.updateMyProfile(
                keycloakSubject = keycloakSub,
                email = null,
                request = UpdateMyProfileRequest(phoneNumber = "+49 123"),
            )
        assertEquals(LocalDate.of(1992, 12, 7), preserved.birthDate)
    }
```

- [ ] **Step 2: Run them to verify they fail (compile error — fields don't exist)**

```bash
./gradlew test --tests "com.hanmaum.dn.app.features.members.api.MemberMappersTest" --tests "com.hanmaum.dn.app.features.members.service.MemberServiceTest"
```

Expected: compilation FAILS on `response.division` / `UpdateMyProfileRequest(birthDate = ...)`.

- [ ] **Step 3: Extend the DTOs**

In `MemberDtos.kt`, `MemberResponse`: add after `groupName`:

```kotlin
    /** Division of the member's church group (e.g. "2교구"), or null when ungrouped. */
    val division: String? = null,
```

and after `phoneNumber`:

```kotlin
    val birthDate: LocalDate? = null,
```

In `UpdateMyProfileRequest`, add after `phoneNumber`:

```kotlin
    val birthDate: LocalDate? = null,
```

(`LocalDate` is already imported in this file.)

- [ ] **Step 4: Map and apply**

`MemberMappers.kt`, inside `Member.toResponse()` add:

```kotlin
        division = this.group?.division,
        birthDate = this.birthDate,
```

`MemberService.kt`, inside `updateMyProfile` after the `phoneNumber` line (~272):

```kotlin
        request.birthDate?.let { member.birthDate = it }
```

- [ ] **Step 5: Run the tests to verify they pass, then the full suite**

```bash
./gradlew test --tests "com.hanmaum.dn.app.features.members.api.MemberMappersTest" --tests "com.hanmaum.dn.app.features.members.service.MemberServiceTest"
./gradlew test
```

Expected: all green.

- [ ] **Step 6: Commit + PR (server conventions)**

```bash
git add src/main/kotlin/com/hanmaum/dn/app/features/members src/test/kotlin/com/hanmaum/dn/app/features/members
git commit -m "feat(members): expose division and birthDate on /members/me; accept birthDate in PATCH

The mobile profile redesign shows the member's church-group division and
birthdate, and lets members edit the birthdate. Null keeps the old value,
consistent with the other PATCH fields."
git push -u origin feat/HDN-0-member-me-division-birthdate
```

Open the PR with the server repo's `/pr` template. Mobile Tasks 2-7 do NOT block on this merge (null-tolerant), but deploy the server before releasing the mobile build.

---

### Task 2: MOBILE — mirror the contract: DTOs, repository, wire tests

**Files (mobile repo, paths relative to `composeApp/src/`):**
- Modify: `commonMain/kotlin/com/hanmaum/dn/mobile/features/member/data/model/MemberResponse.kt`
- Modify: `commonMain/kotlin/com/hanmaum/dn/mobile/features/member/data/model/UpdateMyProfileRequest.kt`
- Modify: `commonMain/kotlin/com/hanmaum/dn/mobile/features/member/domain/repository/MemberRepository.kt`
- Modify: `commonMain/kotlin/com/hanmaum/dn/mobile/features/member/data/repository/MemberRepositoryImpl.kt`
- Modify: `commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileViewModel.kt` (call-site compile fix only; real value in Task 4)
- Test: `commonTest/kotlin/com/hanmaum/dn/mobile/features/member/data/repository/MemberRepositoryImplTest.kt` (exists since PR #83)

**Interfaces:**
- Produces (Tasks 4/6 rely on): `MemberResponse.division: String?`, `MemberResponse.birthDate: String?` (ISO `"YYYY-MM-DD"`); repository signature:

```kotlin
    suspend fun updateMyProfile(
        phoneNumber: String?,
        profileImageUrl: String?,
        birthDate: String?,
        street: String?,
        houseNumber: String?,
        zipCode: String?,
        city: String?,
    ): Result<MemberResponse>
```

- [ ] **Step 1: Write the failing tests** — add to `MemberRepositoryImplTest.kt` (reuse its existing `mockClient`/`member` fixture; extend the `member` fixture with `division = "2교구", birthDate = "1992-12-07"` once the DTO compiles — write tests first):

```kotlin
    @Test
    fun getMyProfile_mapsDivisionAndBirthDate() = runTest {
        val json = """
            {"success":true,"data":{"publicId":"u1","firstName":"Seungjin","lastName":"Kim",
             "status":"ACTIVE","groupName":"다니엘조","division":"2교구","birthDate":"1992-12-07"}}
        """.trimIndent()
        val p = MemberRepositoryImpl(mockClient(json)).getMyProfile().getOrThrow()
        assertEquals("2교구", p.division)
        assertEquals("1992-12-07", p.birthDate)
    }

    @Test
    fun getMyProfile_toleratesOldBackendWithoutNewFields() = runTest {
        // Response shape from a backend that has not deployed the contract PR yet.
        val json = """
            {"success":true,"data":{"publicId":"u1","firstName":"Seungjin","lastName":"Kim","status":"ACTIVE"}}
        """.trimIndent()
        val p = MemberRepositoryImpl(mockClient(json)).getMyProfile().getOrThrow()
        assertEquals(null, p.division)
        assertEquals(null, p.birthDate)
    }

    @Test
    fun updateMyProfile_sendsBirthDateKeyCamelCase() = runTest {
        var body = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req -> body = (req.body as TextContent).text }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, profileImageUrl = null, birthDate = "1992-12-07",
            street = null, houseNumber = null, zipCode = null, city = null,
        )
        assertTrue(body.contains("\"birthDate\":\"1992-12-07\""), "body was $body")
    }

    @Test
    fun updateMyProfile_omitsNullBirthDate() = runTest {
        var body = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req -> body = (req.body as TextContent).text }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, profileImageUrl = null, birthDate = null,
            street = null, houseNumber = null, zipCode = null, city = "Köln",
        )
        assertFalse(body.contains("birthDate"), "body was $body")
    }
```

Also update the two existing `updateMyProfile_*` tests from PR #83 to pass the new `birthDate = null` argument (named args — insert between `profileImageUrl` and `street`).

- [ ] **Step 2: Run to verify compile failure**

```bash
./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.member.data.repository.MemberRepositoryImplTest"
```

- [ ] **Step 3: Implement**

`MemberResponse.kt`: add `val division: String? = null,` after `groupName`, and `val birthDate: String? = null,` after `phoneNumber`.

`UpdateMyProfileRequest.kt`: add `val birthDate: String? = null,` after `profileImageUrl` (keep the KDoc note about camelCase; extend it with: birthDate is the ISO string `"YYYY-MM-DD"`).

`MemberRepository.kt` + `MemberRepositoryImpl.kt`: add `birthDate: String?` parameter after `profileImageUrl`, pass through to `UpdateMyProfileRequest(birthDate = birthDate, ...)`.

`ProfileViewModel.saveProfile()`: add `birthDate = null,` after the `profileImageUrl` argument (temporary — Task 4 replaces; no marker comment, the word TODO fails CI).

- [ ] **Step 4: Run the class, then the full suite**

```bash
./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.member.data.repository.MemberRepositoryImplTest"
./gradlew :composeApp:testDevDebugUnitTest
```

Expected: all green.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/member \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileViewModel.kt \
        composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/member
git commit -m "feat(member): division and birthDate in profile contract

Mirrors server PR feat/HDN-0-member-me-division-birthdate. Parsing is
null-tolerant so the app keeps working against an un-upgraded backend."
```

---

### Task 3: MOBILE — extract `BirthdayField` to a shared component

**Files:**
- Create: `commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/BirthdayField.kt`
- Modify: `commonMain/kotlin/com/hanmaum/dn/mobile/features/login/presentation/RegisterScreen.kt` (remove the private composable ~lines 567-640, import the shared one)

**Interfaces:**
- Produces: `@Composable fun BirthdayField(value: String, onValueChange: (String) -> Unit, error: String? = null)` — value/display format `"yyyy.MM.dd"`, opens a Material3 `DatePickerDialog`, auto-inserts `.` separators (caret-preserving `TextFieldValue` logic).

- [ ] **Step 1: Move the composable verbatim**

Cut the entire `private fun BirthdayField(...)` from `RegisterScreen.kt` (it starts at the `// ── Form field helpers` comment, ~line 565) into the new file, package `com.hanmaum.dn.mobile.core.presentation.components`, visibility `public` (drop `private`). Move any private helpers it references (and only those — if a helper is shared with other Register fields, leave it and import instead). Copy the needed imports (Material3 DatePicker*, kotlinx-datetime `Instant`/`TimeZone`, `TextFieldValue`/`TextRange`). Do not change behavior.

- [ ] **Step 2: Point RegisterScreen at the shared component**

Add `import com.hanmaum.dn.mobile.core.presentation.components.BirthdayField` to `RegisterScreen.kt`; delete the moved code. If the moved composable referenced Register-local strings for its label, keep that text passed in from the call site or as-is — behavior identical.

- [ ] **Step 3: Verify by compiling + full unit suite**

```bash
./gradlew :composeApp:assembleDevDebug && ./gradlew :composeApp:testDevDebugUnitTest
```

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/BirthdayField.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/login/presentation/RegisterScreen.kt
git commit -m "refactor(ui): extract BirthdayField for reuse on the personal-info screen"
```

---

### Task 4: MOBILE — ViewModel/UiState: editBirthDate, isDirty, save/reset

**Files:**
- Modify: `commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileUiState.kt`
- Modify: `commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileViewModel.kt`
- Test: `commonTest/kotlin/com/hanmaum/dn/mobile/features/profile/ProfileViewModelTest.kt` (exists since PR #83)

**Interfaces:**
- Produces (Task 6 relies on): `ProfileUiState.Success.editBirthDate: String` (display `"yyyy.MM.dd"`), `Success.isDirty: Boolean`, `Success.saveSuccess: Boolean`, `ProfileViewModel.updateBirthDate(String)`, `ProfileViewModel.resetEdits()`, `ProfileViewModel.consumeSaveSuccess()`. `isEditing`, `startEditing()`, `cancelEditing()` are REMOVED (view = edit; there is no mode anymore).

- [ ] **Step 1: Write the failing tests** — in `ProfileViewModelTest.kt`, replace the `startEditing`/`cancelEditing`-based tests and add (fakes from PR #83 stay; add `birthDate = "1992-12-07"`, `division = "2교구"` to the fake's profile fixture):

```kotlin
    @Test
    fun `load seeds edit birth date in display format`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("1992.12.07", success(viewModel).editBirthDate)
    }

    @Test
    fun `state is not dirty after load and dirty after an edit`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, success(viewModel).isDirty)
        viewModel.updatePhone("+49 999")
        assertEquals(true, success(viewModel).isDirty)
    }

    @Test
    fun `save converts birth date to iso format`() = runTest {
        val repo = FakeMemberRepository()
        val viewModel = vm(repo)
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.updateBirthDate("1990.01.31")
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("1990-01-31", repo.lastUpdate?.birthDate)
    }

    @Test
    fun `save sets saveSuccess and state is clean again`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.updateCity("Bonn")
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, success(viewModel).saveSuccess)
        assertEquals(false, success(viewModel).isDirty)
        viewModel.consumeSaveSuccess()
        assertEquals(false, success(viewModel).saveSuccess)
    }

    @Test
    fun `reset edits restores loaded values`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.updateHouseNumber("99")
        viewModel.resetEdits()
        assertEquals("12", success(viewModel).editHouseNumber)
        assertEquals(false, success(viewModel).isDirty)
    }

    @Test
    fun `silent refresh does not clobber a dirty edit`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.updatePhone("+49 111")
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("+49 111", success(viewModel).editPhone)
    }
```

Also update `FakeMemberRepository.updateMyProfile` and `UpdateArgs` with the `birthDate: String?` parameter (mirror null=keep like the other fields).

- [ ] **Step 2: Run to verify compile failure**

```bash
./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.profile.ProfileViewModelTest"
```

- [ ] **Step 3: Implement the state** — replace `ProfileUiState.Success` with:

```kotlin
    data class Success(
        val profile: MemberResponse,
        val editPhone: String = profile.phoneNumber ?: "",
        val editImageUrl: String = profile.profileImageUrl ?: "",
        val editBirthDate: String = profile.birthDate?.replace('-', '.') ?: "",
        val editStreet: String = profile.street ?: "",
        val editHouseNumber: String = profile.houseNumber ?: "",
        val editZipCode: String = profile.zipCode ?: "",
        val editCity: String = profile.city ?: "",
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val saveSuccess: Boolean = false,
    ) : ProfileUiState() {
        /** True when any edit field differs from the loaded profile — gates the Save button. */
        val isDirty: Boolean
            get() = editPhone != (profile.phoneNumber ?: "") ||
                editImageUrl != (profile.profileImageUrl ?: "") ||
                editBirthDate != (profile.birthDate?.replace('-', '.') ?: "") ||
                editStreet != (profile.street ?: "") ||
                editHouseNumber != (profile.houseNumber ?: "") ||
                editZipCode != (profile.zipCode ?: "") ||
                editCity != (profile.city ?: "")
    }
```

- [ ] **Step 4: Implement the ViewModel changes** in `ProfileViewModel.kt`:
  - Delete `startEditing()`/`cancelEditing()`; add:

```kotlin
    fun updateBirthDate(value: String) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(editBirthDate = value)
    }

    /** Discards in-progress edits, re-seeding every edit field from the loaded profile. */
    fun resetEdits() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = ProfileUiState.Success(current.profile)
    }

    fun consumeSaveSuccess() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        if (current.saveSuccess) _uiState.value = current.copy(saveSuccess = false)
    }
```

  - `loadProfile()` guard: replace the `isEditing` check with `if (current is ProfileUiState.Success && current.isDirty) return@launch` (update the KDoc: refresh skips while an edit is in progress).
  - `saveProfile()`: replace the Task-2 `birthDate = null` with `birthDate = current.editBirthDate.replace('.', '-').takeIf { it.length == 10 },` and on success set `ProfileUiState.Success(updated).copy(saveSuccess = true)`.

- [ ] **Step 5: Run the class, then full suite; fix any compile fallout in `ProfileScreen.kt` minimally** (the screen still calls `startEditing`/`cancelEditing` — Task 6 rewrites it; for THIS task, change those two call sites to `viewModel.resetEdits()` / remove, keeping behavior compilable). Expected: all green.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile \
        composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/profile
git commit -m "feat(profile): dirty-tracked single-mode edit state with birthdate

View=edit means there is no edit mode: the state always carries prefilled
edit fields, isDirty gates Save, and a silent refresh never clobbers an
in-progress edit."
```

---

### Task 5: MOBILE — routes, strings, Settings screen

**Files:**
- Modify: `commonMain/kotlin/com/hanmaum/dn/mobile/core/navigation/Routes.kt`
- Modify: `commonMain/kotlin/com/hanmaum/dn/mobile/core/i18n/AppStrings.kt` (interface + Ko + En + De, all in this one file)
- Create: `commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/SettingsScreen.kt`
- Modify: `commonMain/kotlin/com/hanmaum/dn/mobile/App.kt`

**Interfaces:**
- Produces: `@Serializable object SettingsRoute`, `@Serializable object PersonalInfoRoute` (Task 6 uses the latter);

```kotlin
@Composable
fun SettingsScreen(
    currentLocale: AppLocale,
    onLocaleChange: (AppLocale) -> Unit,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    keepSignedIn: Boolean,
    onKeepSignedInToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
)
```

- [ ] **Step 1: Add routes** — in `Routes.kt` after `ProfileRoute`:

```kotlin
@Serializable object PersonalInfoRoute
@Serializable object SettingsRoute
```

- [ ] **Step 2: Add strings** — grep `AppStrings.kt` for collisions first (`settings`, `personalInfo`, `labelBirthDate`, `labelDivision`, `labelGroup` — reuse any that already exist). Add to the interface + all three impls:

| key | KO | EN | DE |
|---|---|---|---|
| `settingsTitle` | 설정 | Settings | Einstellungen |
| `personalInfoTitle` | 개인 정보 | Personal Info | Persönliche Daten |
| `labelBirthDate` | 생년월일 | BIRTH DATE | GEBURTSDATUM |
| `labelDivision` | 교구 | DIVISION | BEZIRK |
| `labelGroup` | 목장 | GROUP | GRUPPE |
| `lockedFieldHint` | 교회에서 관리하는 정보입니다 | Managed by the church office | Wird vom Gemeindebüro verwaltet |
| `profileSaved` | 저장되었습니다 | Saved | Gespeichert |

(Missing impl = compile error, which is the guard.)

- [ ] **Step 3: Create `SettingsScreen.kt` by MOVING the existing settings UI** — everything in today's `ProfileViewContent` below the info cards (the locale row + language picker dialog, theme row + picker dialog, Face ID toggle row, keep-signed-in row, and their `showLanguagePicker`/`showThemePicker` state and helper composables like the settings row/card composables) moves verbatim into `SettingsScreen`, wrapped in detail-screen chrome:

```kotlin
@Composable
fun SettingsScreen(/* signature above */) {
    val strings = LocalStrings.current
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Same chevron-back top bar pattern as MinistryDetailScreen / AnnouncementDetailScreen:
            // IconButton(onClick = onBack, 44dp target) { Icon(Icons.AutoMirrored.Filled.ArrowBack, ...) }
            // + Text(strings.settingsTitle, style = MaterialTheme.typography.titleLarge)
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            // moved language/theme/FaceID/keep-signed-in rows + dialogs, unchanged
        }
    }
}
```

Copy the exact back-bar implementation from an existing detail screen (ministry detail is the reference) rather than inventing one — swipe-back works automatically when the screen is a pushed destination.

- [ ] **Step 4: Register in `App.kt`** — inside the `composable<ProfileRoute>` block, the locale/theme/biometric/keep-signed-in params MOVE to a new block (the surrounding `locale`/`themeMode`/`biometricEnabled`/`keepSignedIn` state and repos are already in scope in `App`):

```kotlin
                    composable<SettingsRoute> {
                        SettingsScreen(
                            currentLocale = locale,
                            onLocaleChange = { newLocale ->
                                localeRepo.setLocale(newLocale)
                                locale = newLocale
                            },
                            currentTheme = themeMode,
                            onThemeChange = { newMode ->
                                themeRepo.setThemeMode(newMode)
                                themeMode = newMode
                            },
                            biometricEnabled = biometricEnabled,
                            biometricAvailable = biometric.isAvailable(),
                            onBiometricToggle = { enable ->
                                if (enable) {
                                    tokenStorage.setBiometricEnabled(true)
                                    biometricEnabled = true
                                } else {
                                    tokenStorage.setBiometricEnabled(false)
                                    credentialStore.clear()
                                    biometricEnabled = false
                                }
                            },
                            keepSignedIn = keepSignedIn,
                            onKeepSignedInToggle = { enable ->
                                tokenStorage.setKeepSignedIn(enable)
                                keepSignedIn = enable
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
```

`ProfileScreen`'s registration keeps compiling in this task by leaving its old signature in place (Task 6 slims it) — the settings UI is duplicated for exactly one commit, which is acceptable because Task 6 removes the profile-side copy in the next commit.

- [ ] **Step 5: Compile + full suite** (`assembleDevDebug` + `testDevDebugUnitTest`). Expected: green.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/SettingsScreen.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt
git commit -m "feat(settings): dedicated settings screen with locale, theme, Face ID, keep-signed-in"
```

---

### Task 6: MOBILE — hub rework + Personal-Info screen

**Files:**
- Modify: `commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileScreen.kt` (becomes the hub; `ProfileEditContent`, the info-card list, and the moved settings blocks are DELETED)
- Create: `commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/PersonalInfoScreen.kt`
- Modify: `commonMain/kotlin/com/hanmaum/dn/mobile/App.kt`

**Interfaces:**
- Consumes: `ProfileUiState.Success` fields incl. `isDirty`/`saveSuccess` (Task 4), `BirthdayField` (Task 3), `PersonalInfoRoute`/`SettingsRoute` + strings (Task 5).
- Produces:

```kotlin
@Composable fun ProfileScreen(
    onLogout: () -> Unit,
    onOpenPersonalInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
)
@Composable fun PersonalInfoScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
)
```

- [ ] **Step 1: Rewrite `ProfileScreen` as the hub.** Structure (keep the existing `AppTopBar`, loading/error branches, logout LaunchedEffect):

```kotlin
// Success branch content, replacing ProfileViewContent/ProfileEditContent:
Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Spacer(Modifier.height(16.dp))
    // Avatar: keep the existing AccountCircle icon block (100.dp, primary tint)
    Spacer(Modifier.height(12.dp))
    Text("${profile.lastName} ${profile.firstName}",
        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground)
    profile.division?.let {                                  // division under the name
        Spacer(Modifier.height(4.dp))
        Text(it, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    profile.groupName?.let {                                 // group-name chip
        Spacer(Modifier.height(8.dp))
        Surface(shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(it, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
    Spacer(Modifier.height(28.dp))
    HubMenuRow(icon = Icons.Default.Person, label = strings.personalInfoTitle, onClick = onOpenPersonalInfo)
    Spacer(Modifier.height(12.dp))
    HubMenuRow(icon = Icons.Default.Settings, label = strings.settingsTitle, onClick = onOpenSettings)
    Spacer(Modifier.height(28.dp))
    // Log out: keep the existing logout button block verbatim
    Spacer(Modifier.height(80.dp)) // pill-nav clearance (space_bottom_nav)
}
```

`HubMenuRow` = full-width `Surface(color = surfaceVariant, shape = shapes.small)` row with leading icon, label (`titleMedium`), trailing chevron (`Icons.AutoMirrored.Filled.KeyboardArrowRight`), press feedback scale 0.97 + `spring()` — copy the press-feedback modifier pattern from an existing card in the codebase (home/attendance cards are the reference; grep `scale(` + `animateFloatAsState` under `features/`). NO status chip, NO churchRole line, NO info cards, NO edit button. Delete `ProfileEditContent`, `InfoCard` usages for profile data, the settings blocks (now in SettingsScreen), and the now-unused imports/params (`currentLocale`, `onLocaleChange`, `currentTheme`, `onThemeChange`, `biometricEnabled`, `biometricAvailable`, `onBiometricToggle`, `keepSignedIn`, `onKeepSignedInToggle`).

- [ ] **Step 2: Create `PersonalInfoScreen.kt`.** Detail chrome (same back bar as SettingsScreen), `LaunchedEffect(Unit) { viewModel.loadProfile() }`, and for the Success state a `verticalScroll` + `imePadding` + `dismissKeyboardOnTap` Column containing:
  1. **Locked section** (on `surface_container_low` via a `Surface`, No-Line Rule): rows for name, email, division ("—" when null), group ("—" when null), church role ("—" when null) — each a label (`labelSmall`, UPPERCASE key from strings: reuse `labelEmail` etc.) + value (`bodyLarge`, `onSurfaceVariant`) + trailing `Icons.Default.Lock` (16.dp, `muted`-tone `outline` color). Under the section: `strings.lockedFieldHint` in `bodySmall` + `outline`.
  2. **Editable section**: the exact field-block pattern from the old `ProfileEditContent` (label + `TextField` with surfaceVariant containers, transparent indicators, 0.35f placeholders) for: image URL (`strings.profileImageUrl`), phone (`strings.labelPhone`), then `BirthdayField(value = state.editBirthDate, onValueChange = viewModel::updateBirthDate)` under a `strings.labelBirthDate` label, then street / house number / zip / city exactly as they exist today (reuse the code you are deleting from `ProfileEditContent` — move, don't retype).
  3. `state.saveError` text (error color) as before; then the Save button:

```kotlin
Button(
    onClick = { viewModel.saveProfile() },
    enabled = state.isDirty && !state.isSaving,
    modifier = Modifier.fillMaxWidth().height(50.dp),
    shape = MaterialTheme.shapes.extraSmall,
) { Text(strings.save, style = MaterialTheme.typography.labelLarge) }
```

(grep `AppStrings` for an existing `save`/`profileSave` key before adding one.)
  4. Save confirmation: `LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) { snackbarHostState.showSnackbar(strings.profileSaved); viewModel.consumeSaveSuccess() } }` with a `SnackbarHost` in the Scaffold.
  5. Back discards: `onBack` just pops — in-progress edits die with the destination's ViewModel (each `koinViewModel()` is scoped to its backstack entry), which is the confirmed "back = discard" UX. Do not add a confirm dialog.

- [ ] **Step 3: Wire `App.kt`** — slim the `composable<ProfileRoute>` block to:

```kotlin
                    composable<ProfileRoute> {
                        ProfileScreen(
                            onLogout = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onOpenPersonalInfo = { navController.navigate(PersonalInfoRoute) },
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                        )
                    }

                    composable<PersonalInfoRoute> {
                        PersonalInfoScreen(onBack = { navController.popBackStack() })
                    }
```

- [ ] **Step 4: Compile, full unit suite, then commit**

```bash
./gradlew :composeApp:assembleDevDebug && ./gradlew :composeApp:testDevDebugUnitTest
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt
git commit -m "feat(profile): hub tab + personal-info screen (view=edit)

Profile tab now shows identity only (name, division, group chip) with
menu rows; personal data lives once, on a prefilled editable form with
locked admin-managed rows and a dirty-gated Save."
```

---

### Task 7: Verification gate, screenshots, docs, PR

- [ ] **Step 1: Full §7 gate (show output)**

```bash
./gradlew :composeApp:testDevDebugUnitTest
grep -rn "TODO" composeApp/src || echo "no TODOs"
./gradlew lint          # baseline as of 2026-07-09: 0 errors / 45 warnings — nothing NEW allowed
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test
```

- [ ] **Step 2: iOS simulator run + screenshots** (build/install/launch per CLAUDE.md §4). Verify launch without crash; capture hub, Settings, Personal-Info screenshots in light AND dark.

- [ ] **Step 3: Android emulator against the real dev backend (needs the server PR deployed for division/birthdate to show; without it, verify the rows hide gracefully).** Walk: hub shows division + group chip (no address/phone) → Settings screen toggles work and persist → Personal-Info shows locked rows + prefilled fields → change birthdate via picker + one address field → Save enables only after the change → save → snackbar → back → re-enter → values persisted. Screenshots for the PR.

- [ ] **Step 4: Update `../dn-app/MVP.md`** — profile feature row status only (never delete rows).

- [ ] **Step 5: Push + PR to `develop`** (mobile), title `feat(profile): hub tab, settings screen, personal-info view=edit redesign`; body: why (user feedback: no personal data on the tab) / what (three screens, contract extension, BirthdayField reuse) / how tested (gate output + both-platform screenshots + note whether the server PR was deployed during testing). Merge only via `gh pr checks <n> --watch --fail-fast && gh pr merge <n> --squash --delete-branch`.

---

## Explicitly out of scope (from the spec)

- Gallery photo picker + S3 upload (separate feature; backend missing).
- Editing name/email/group/division/role from the app (admin-managed).
- Notification preferences (nothing to configure yet).
- Clearing a field to empty (null=keep PATCH semantics — pre-existing limitation).
