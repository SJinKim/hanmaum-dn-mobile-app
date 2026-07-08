# Profile Edit House-Number Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the profile-edit bug where the house number (Hausnummer) is missing from the edit form and ends up duplicated in the profile view after saving.

**Architecture:** Pure mobile-side fix in the existing `member` (data) and `profile` (presentation) feature slices — no backend change needed. Add `houseNumber` end-to-end through `UpdateMyProfileRequest` → `MemberRepository` → `ProfileUiState`/`ProfileViewModel` → `ProfileEditContent`, and fix a second latent wire-contract bug (`zip_code` vs `zipCode`) in the same DTO.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.10 (Material3), Ktor 3 client + kotlinx-serialization, Koin (no DI changes needed — bindings already exist), kotlin-test + kotlinx-coroutines-test + Ktor MockEngine.

## Root cause (user-reported bug, verified in code)

User report (German): *"주소, 우편번호, 도시 come up but house number is gone. When I enter the Hausnummer and save, it is applied and then my Hausnummer appears twice."*

Verified chain:

1. The backend keeps `street` and `houseNumber` as **separate fields**
   (`../hanmaum-dn-server/src/main/kotlin/com/hanmaum/dn/app/features/members/api/v1/dto/MemberDtos.kt:206-215` — `UpdateMyProfileRequest` accepts `houseNumber`; `PATCH /members/me` applies each non-null field via `request.x?.let { member.x = it }` in `MemberService.kt:265-279`, i.e. **null = keep old value**).
2. The mobile read DTO `MemberResponse` has `houseNumber` and the profile **view** renders it as its own card (`ProfileScreen.kt:233-235`).
3. But the profile **edit** path lost the field everywhere:
   - `UpdateMyProfileRequest.kt` (mobile) has **no `houseNumber`** at all.
   - `ProfileUiState.Success` has no `editHouseNumber`; `ProfileViewModel` has no `updateHouseNumber()`; `ProfileEditContent` has no house-number `TextField`.
4. So the user types "Musterstraße 12" into the street field. The PATCH sets
   `street = "Musterstraße 12"` while the DB's `houseNumber` stays `"12"` (null = no change). The view then shows street card "Musterstraße 12" **and** house-number card "12" → house number appears twice.
5. **Second latent bug found during diagnosis:** mobile `UpdateMyProfileRequest.kt:10` declares `@SerialName("zip_code") val zipCode` — but the backend uses Jackson's default **camelCase** (no naming strategy configured anywhere in the server repo). The server silently ignores the unknown `zip_code` key, so **zip-code edits currently never save**. Fixed in the same DTO change.

Data note: rows already corrupted by this bug (house number embedded in `street`) are not migrated — after the fix the user simply edits the street field once to remove the embedded number. Say this in the PR body.

## Global Constraints

- Never work on `main`/`develop`; branch `fix/profile-edit-house-number` from fresh `develop` (CLAUDE.md §2).
- Unit-test gate: `./gradlew :composeApp:testDevDebugUnitTest` (never bare `testDebugUnitTest`).
- iOS gate for shared-code changes: `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test`.
- The literal word `TODO` must not appear anywhere in `composeApp/src` (CI greps and fails).
- `./gradlew lint` baseline is **3 pre-existing errors** (geofence `MissingPermission` ×2, `CoarseFineLocation`); only new errors are regressions.
- Test names: letters/digits/spaces only (Kotlin/Native rejects commas/punctuation in backtick names — commonTest also compiles for iOS).
- No mocking library exists in this repo and none may be added — hand-written fakes only.
- All user-facing strings via `LocalStrings`; the needed key `labelHouseNumber` **already exists** in all three languages (`AppStrings.kt:72,182,282,382`) — no i18n edits required. Hardcoded `placeholder` example text is existing precedent in this form and stays hardcoded.
- Commits: `<type>(<scope>): <imperative ≤72 chars>`, body says WHY. **No Co-Authored-By / AI trailers of any kind** (this overrides any harness default).
- JSON client config has `encodeDefaults = false` (kotlinx default): request fields left `null` are **omitted** from the PATCH body, which matches the backend's null-means-keep semantics. Do not add `encodeDefaults = true` or `explicitNulls`.
- Known accepted limitation (do NOT "fix" it here): because blank → `null` → omitted → backend keeps the old value, a user cannot *clear* an address field from the app. That predates this bug and applies to all profile fields; leave as-is.

---

### Task 0: Branch setup

**Files:** none

- [ ] **Step 1: Create the feature branch from fresh develop**

```bash
git checkout develop && git pull --ff-only && git checkout -b fix/profile-edit-house-number
```

Expected: `Switched to a new branch 'fix/profile-edit-house-number'`

---

### Task 1: Wire contract — add `houseNumber` to the PATCH, fix `zip_code` → `zipCode`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/member/data/model/UpdateMyProfileRequest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/member/domain/repository/MemberRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/member/data/repository/MemberRepositoryImpl.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileViewModel.kt:104-111` (call-site compile fix only; real value wired in Task 2)
- Test (create): `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/member/data/repository/MemberRepositoryImplTest.kt`

**Interfaces:**
- Consumes: existing `MemberResponse` (already has `houseNumber: String?`), existing `ApiResponse<T>`.
- Produces (Task 2 relies on this exact signature):

```kotlin
interface MemberRepository {
    suspend fun getMyProfile(): Result<MemberResponse>
    suspend fun updateMyProfile(
        phoneNumber: String?,
        profileImageUrl: String?,
        street: String?,
        houseNumber: String?,
        zipCode: String?,
        city: String?,
    ): Result<MemberResponse>
}
```

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/member/data/repository/MemberRepositoryImplTest.kt` (pattern copied from `MinistryRepositoryImplTest.kt` in the sibling feature):

```kotlin
package com.hanmaum.dn.mobile.features.member.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val testJson = Json { ignoreUnknownKeys = true }

private fun mockClient(
    responseJson: String,
    onRequest: ((HttpRequestData) -> Unit)? = null,
): HttpClient = HttpClient(MockEngine { request ->
    onRequest?.invoke(request)
    respond(
        content = responseJson,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}) {
    install(ContentNegotiation) { json(testJson) }
    defaultRequest {
        if (url.host.isBlank()) {
            val path = url.encodedPath.removePrefix("/")
            url.takeFrom("http://localhost")
            url.encodedPath = "/$path"
        }
    }
}

class MemberRepositoryImplTest {

    private val member = MemberResponse(
        publicId = "u1",
        firstName = "Seungjin",
        lastName = "Kim",
        status = MemberStatus.ACTIVE,
        street = "Musterstraße",
        houseNumber = "12",
        zipCode = "50667",
        city = "Köln",
    )

    @Test
    fun getMyProfile_mapsHouseNumber() = runTest {
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val result = MemberRepositoryImpl(mockClient(json)).getMyProfile()
        val p = result.getOrThrow()
        assertEquals("Musterstraße", p.street)
        assertEquals("12", p.houseNumber)
    }

    @Test
    fun updateMyProfile_patchesMembersMe() = runTest {
        var method: HttpMethod? = null
        var path = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req ->
            method = req.method
            path = req.url.encodedPath
        }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, profileImageUrl = null,
            street = "Musterstraße", houseNumber = "12", zipCode = "50667", city = "Köln",
        )
        assertEquals(HttpMethod.Patch, method)
        assertEquals("/members/me", path)
    }

    @Test
    fun updateMyProfile_sendsHouseNumberAndCamelCaseZipCode() = runTest {
        // Regression for the Hausnummer bug: the old request DTO had no
        // houseNumber field at all and misnamed zipCode as zip_code (the
        // backend is default-Jackson camelCase, so zip_code was silently
        // dropped server-side).
        var body = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req -> body = (req.body as TextContent).text }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, profileImageUrl = null,
            street = "Musterstraße", houseNumber = "12", zipCode = "50667", city = "Köln",
        )
        assertTrue(body.contains("\"houseNumber\":\"12\""), "body was $body")
        assertTrue(body.contains("\"zipCode\":\"50667\""), "body was $body")
        assertFalse(body.contains("zip_code"), "body was $body")
    }

    @Test
    fun updateMyProfile_omitsNullFieldsFromBody() = runTest {
        // Backend PATCH semantics are null-means-keep; kotlinx must OMIT null
        // fields (encodeDefaults=false), not send explicit nulls.
        var body = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req -> body = (req.body as TextContent).text }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, profileImageUrl = null,
            street = null, houseNumber = null, zipCode = null, city = "Köln",
        )
        assertFalse(body.contains("houseNumber"), "body was $body")
        assertFalse(body.contains("street"), "body was $body")
        assertTrue(body.contains("\"city\":\"Köln\""), "body was $body")
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.member.data.repository.MemberRepositoryImplTest"
```

Expected: **compilation FAILS** — `updateMyProfile` has no `houseNumber` parameter yet. (A signature change means the regression test fails at compile time on the old code; that is the expected "fails on old code" evidence.)

- [ ] **Step 3: Fix the request DTO**

Replace the full contents of `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/member/data/model/UpdateMyProfileRequest.kt` with:

```kotlin
package com.hanmaum.dn.mobile.features.member.data.model

import kotlinx.serialization.Serializable

/**
 * PATCH /members/me body. Wire names are camelCase — the backend is default
 * Jackson (no snake_case strategy); a snake_case key is silently ignored.
 * Null fields are omitted (encodeDefaults=false) and mean "keep old value".
 */
@Serializable
data class UpdateMyProfileRequest(
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
    val street: String? = null,
    val houseNumber: String? = null,
    val zipCode: String? = null,
    val city: String? = null,
)
```

(Note this **removes** the wrong `@SerialName("zip_code")` annotation.)

- [ ] **Step 4: Add the parameter to the repository interface**

In `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/member/domain/repository/MemberRepository.kt`, change `updateMyProfile` to:

```kotlin
    suspend fun updateMyProfile(
        phoneNumber: String?,
        profileImageUrl: String?,
        street: String?,
        houseNumber: String?,
        zipCode: String?,
        city: String?,
    ): Result<MemberResponse>
```

- [ ] **Step 5: Pass it through the implementation**

In `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/member/data/repository/MemberRepositoryImpl.kt`, change the `updateMyProfile` signature and body construction to:

```kotlin
    override suspend fun updateMyProfile(
        phoneNumber: String?,
        profileImageUrl: String?,
        street: String?,
        houseNumber: String?,
        zipCode: String?,
        city: String?,
    ): Result<MemberResponse> {
        return try {
            val response = client.patch("members/me") {
                contentType(ContentType.Application.Json)
                setBody(UpdateMyProfileRequest(
                    phoneNumber = phoneNumber,
                    profileImageUrl = profileImageUrl,
                    street = street,
                    houseNumber = houseNumber,
                    zipCode = zipCode,
                    city = city,
                ))
            }
            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<ApiResponse<MemberResponse>>()
                val member = apiResponse.data
                    ?: return Result.failure(Exception("Profile data is null"))
                Result.success(member)
            } else {
                Result.failure(Exception("Profile update failed (${response.status})"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
```

- [ ] **Step 6: Keep the ViewModel call site compiling (temporary)**

In `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileViewModel.kt`, inside `saveProfile()`, add the new argument with a temporary `null` (Task 2 replaces it with the real edit value):

```kotlin
            memberRepository.updateMyProfile(
                phoneNumber = current.editPhone.ifBlank { null },
                profileImageUrl = current.editImageUrl.ifBlank { null },
                street = current.editStreet.ifBlank { null },
                houseNumber = null,
                zipCode = current.editZipCode.ifBlank { null },
                city = current.editCity.ifBlank { null },
            ).fold(
```

(Do NOT write any marker comment next to it — the word "TODO" anywhere in `composeApp/src` fails CI. The `null` is behavior-identical to today and is removed by Task 2.)

- [ ] **Step 7: Run the test to verify it passes**

```bash
./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.member.data.repository.MemberRepositoryImplTest"
```

Expected: `BUILD SUCCESSFUL`, 4 tests pass.

- [ ] **Step 8: Run the full unit suite (catches any other call site)**

```bash
./gradlew :composeApp:testDevDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, no failures.

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/member \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileViewModel.kt \
        composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/member
git commit -m "fix(profile): send houseNumber in PATCH and fix zip_code wire name

The profile PATCH request DTO had no houseNumber field and misnamed
zipCode as zip_code; the backend (default-Jackson camelCase, null=keep)
silently ignored the snake_case key, so zip edits never saved and the
house number could never be updated from the app."
```

---

### Task 2: ViewModel + UiState — expose an editable house number

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileUiState.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileViewModel.kt`
- Test (create): `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/profile/ProfileViewModelTest.kt`

**Interfaces:**
- Consumes: `MemberRepository.updateMyProfile(phoneNumber, profileImageUrl, street, houseNumber, zipCode, city)` from Task 1; `TokenStorage` (`core/domain/repository/TokenStorage.kt`), `CredentialStore`+`SecureStore` (`core/security/`) for constructor fakes.
- Produces (Task 3 relies on these exact names): `ProfileUiState.Success.editHouseNumber: String` and `ProfileViewModel.updateHouseNumber(value: String)`.

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/profile/ProfileViewModelTest.kt`:

```kotlin
package com.hanmaum.dn.mobile.features.profile

import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.security.CredentialStore
import com.hanmaum.dn.mobile.core.security.SecureStore
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.hanmaum.dn.mobile.features.profile.presentation.ProfileUiState
import com.hanmaum.dn.mobile.features.profile.presentation.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakeMemberRepository : MemberRepository {
    var profile = MemberResponse(
        publicId = "u1",
        firstName = "Seungjin",
        lastName = "Kim",
        status = MemberStatus.ACTIVE,
        street = "Musterstraße",
        houseNumber = "12",
        zipCode = "50667",
        city = "Köln",
    )

    data class UpdateArgs(
        val phoneNumber: String?,
        val profileImageUrl: String?,
        val street: String?,
        val houseNumber: String?,
        val zipCode: String?,
        val city: String?,
    )

    var lastUpdate: UpdateArgs? = null

    override suspend fun getMyProfile(): Result<MemberResponse> = Result.success(profile)

    override suspend fun updateMyProfile(
        phoneNumber: String?,
        profileImageUrl: String?,
        street: String?,
        houseNumber: String?,
        zipCode: String?,
        city: String?,
    ): Result<MemberResponse> {
        lastUpdate = UpdateArgs(phoneNumber, profileImageUrl, street, houseNumber, zipCode, city)
        // Mirror backend PATCH semantics: null keeps the old value.
        profile = profile.copy(
            phoneNumber = phoneNumber ?: profile.phoneNumber,
            profileImageUrl = profileImageUrl ?: profile.profileImageUrl,
            street = street ?: profile.street,
            houseNumber = houseNumber ?: profile.houseNumber,
            zipCode = zipCode ?: profile.zipCode,
            city = city ?: profile.city,
        )
        return Result.success(profile)
    }
}

private class FakeTokenStorage : TokenStorage {
    private var access: String? = null
    private var refresh: String? = null
    private var keepSignedIn = true
    private var biometric = false
    override fun saveAccessToken(token: String) { access = token }
    override fun getAccessToken(): String? = access
    override fun saveRefreshToken(token: String?) { refresh = token }
    override fun getRefreshToken(): String? = refresh
    override fun clear() { access = null; refresh = null }
    override fun setKeepSignedIn(value: Boolean) { keepSignedIn = value }
    override fun isKeepSignedIn(): Boolean = keepSignedIn
    override fun setBiometricEnabled(value: Boolean) { biometric = value }
    override fun isBiometricEnabled(): Boolean = biometric
}

private class InMemorySecureStore : SecureStore {
    private val map = mutableMapOf<String, String>()
    override fun putString(key: String, value: String) { map[key] = value }
    override fun getString(key: String): String? = map[key]
    override fun remove(key: String) { map.remove(key) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun vm(repo: MemberRepository) =
        ProfileViewModel(repo, FakeTokenStorage(), CredentialStore(InMemorySecureStore()))

    private fun success(viewModel: ProfileViewModel) =
        viewModel.uiState.value as ProfileUiState.Success

    @Test
    fun `load seeds edit house number from profile`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("12", success(viewModel).editHouseNumber)
    }

    @Test
    fun `update house number changes edit state`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.startEditing()
        viewModel.updateHouseNumber("12a")
        assertEquals("12a", success(viewModel).editHouseNumber)
    }

    @Test
    fun `save sends the edited house number to the repository`() = runTest {
        val repo = FakeMemberRepository()
        val viewModel = vm(repo)
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.startEditing()
        viewModel.updateHouseNumber("12a")
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("12a", repo.lastUpdate?.houseNumber)
        assertEquals("12a", success(viewModel).profile.houseNumber)
    }

    @Test
    fun `save sends null when house number is blank`() = runTest {
        val repo = FakeMemberRepository()
        repo.profile = repo.profile.copy(houseNumber = null)
        val viewModel = vm(repo)
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.startEditing()
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(repo.lastUpdate?.houseNumber)
    }

    @Test
    fun `cancel editing resets house number to profile value`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.startEditing()
        viewModel.updateHouseNumber("99")
        viewModel.cancelEditing()
        assertEquals("12", success(viewModel).editHouseNumber)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.profile.ProfileViewModelTest"
```

Expected: **compilation FAILS** — `editHouseNumber` / `updateHouseNumber` don't exist yet.

- [ ] **Step 3: Add the edit field to the UiState**

In `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileUiState.kt`, add `editHouseNumber` after `editStreet`:

```kotlin
    data class Success(
        val profile: MemberResponse,
        val isEditing: Boolean = false,
        val editPhone: String = profile.phoneNumber ?: "",
        val editImageUrl: String = profile.profileImageUrl ?: "",
        val editStreet: String = profile.street ?: "",
        val editHouseNumber: String = profile.houseNumber ?: "",
        val editZipCode: String = profile.zipCode ?: "",
        val editCity: String = profile.city ?: "",
        val isSaving: Boolean = false,
        val saveError: String? = null,
    ) : ProfileUiState()
```

- [ ] **Step 4: Wire it through the ViewModel**

In `ProfileViewModel.kt`, make three edits:

(a) In `cancelEditing()`, add the reset line after `editStreet`:

```kotlin
            editStreet = current.profile.street ?: "",
            editHouseNumber = current.profile.houseNumber ?: "",
            editZipCode = current.profile.zipCode ?: "",
```

(b) Add the update function next to `updateStreet`:

```kotlin
    fun updateHouseNumber(value: String) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(editHouseNumber = value)
    }
```

(c) In `saveProfile()`, replace the Task-1 temporary `houseNumber = null` with:

```kotlin
                houseNumber = current.editHouseNumber.ifBlank { null },
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.profile.ProfileViewModelTest"
```

Expected: `BUILD SUCCESSFUL`, 5 tests pass.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile \
        composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/profile
git commit -m "fix(profile): editable house number in profile edit state

Users had to type the house number into the street field, which stored
it inside street while the backend kept the old houseNumber — showing
the number twice on the profile view."
```

---

### Task 3: Screen — house-number field in the edit form

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileScreen.kt` (call site ~line 124-133; `ProfileEditContent` ~line 542-682)

**Interfaces:**
- Consumes: `state.editHouseNumber` and `viewModel.updateHouseNumber(it)` from Task 2; existing `strings.labelHouseNumber` (already translated KO/EN/DE).
- Produces: nothing downstream.

There is no Compose UI test rig in this repo; this task is verified by compile + the manual platform verification in Task 4. Match the existing field blocks in this file exactly (they use aligned `=` and `TextFieldDefaults.colors` — keep that style; the theme-token rule is satisfied because all colors/shapes come from `MaterialTheme`).

- [ ] **Step 1: Add the callback parameter to `ProfileEditContent`**

Change the signature (around line 542):

```kotlin
@Composable
private fun ProfileEditContent(
    state: ProfileUiState.Success,
    onPhoneChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onStreetChange: (String) -> Unit,
    onHouseNumberChange: (String) -> Unit,
    onZipCodeChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
```

- [ ] **Step 2: Wire the call site**

At the `ProfileEditContent(` call (around line 124), add the new line between street and zip:

```kotlin
                        ProfileEditContent(
                            state            = state,
                            onPhoneChange    = { viewModel.updatePhone(it) },
                            onImageUrlChange = { viewModel.updateImageUrl(it) },
                            onStreetChange   = { viewModel.updateStreet(it) },
                            onHouseNumberChange = { viewModel.updateHouseNumber(it) },
                            onZipCodeChange  = { viewModel.updateZipCode(it) },
                            onCityChange     = { viewModel.updateCity(it) },
                            onSave           = { viewModel.saveProfile() },
                            onCancel         = { viewModel.cancelEditing() },
                        )
```

- [ ] **Step 3: Add the field to the form + fix the street placeholder**

In the street block (around line 622), change the misleading placeholder — street is street-name-only now:

```kotlin
            placeholder   = { Text("Musterstraße") },
```

Then insert this block directly after the street `TextField` (after line ~634, before the zip-code `Spacer`):

```kotlin
        Spacer(Modifier.height(16.dp))
        Text(
            text     = strings.labelHouseNumber,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        TextField(
            value         = state.editHouseNumber,
            onValueChange = onHouseNumberChange,
            placeholder   = { Text("12a") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            shape         = MaterialTheme.shapes.small,
            colors        = TextFieldDefaults.colors(
                focusedContainerColor      = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor    = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor      = Color.Transparent,
                unfocusedIndicatorColor    = Color.Transparent,
                focusedPlaceholderColor    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                unfocusedPlaceholderColor  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            ),
        )
```

(Keep the keyboard as plain text — German house numbers like "12a" contain letters. Do not add validation here; the profile form has none for other fields, and the backend accepts any ≤50-char string.)

- [ ] **Step 4: Compile Android to verify the screen builds**

```bash
./gradlew :composeApp:assembleDevDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileScreen.kt
git commit -m "fix(profile): house number field in the profile edit form

Street placeholder now shows a street-name-only example so users stop
embedding the house number in the street field."
```

---

### Task 4: Full verification gate + lessons entry + PR

**Files:**
- Modify: `tasks/lessons.md` (append one entry)

- [ ] **Step 1: Run the §7 minimum bar (all four, show output)**

```bash
./gradlew :composeApp:testDevDebugUnitTest
grep -rn "TODO" composeApp/src || echo "no TODOs"
./gradlew lint
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test
```

Expected: tests pass on both JVM and iOS-simulator targets; no TODO matches; lint shows exactly the 3 pre-existing geofence errors and nothing new. (No Swift-visible Kotlin API changed, so the xcodebuild interop gate is not required — the iOS test compile above already compiles the new commonTest code for Kotlin/Native, which is what catches native-invalid test names.)

- [ ] **Step 2: Manual verification on Android emulator against the real dev backend**

Install and launch `assembleDevDebug` on an emulator, log in, open Profile → Edit:
1. The form now shows STREET, HAUSNUMMER (HOUSE NO.), ZIP CODE, CITY.
2. Set street to a plain street name, house number to e.g. `12`, save.
3. Profile view shows street once and house number once — no duplication.
4. Edit again, change only the zip code, save → the new zip persists (this was the silently-broken `zip_code` path).
5. Capture a screenshot of the edit form and the profile view for the PR.

- [ ] **Step 3: iOS simulator smoke run + screenshot**

```bash
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /tmp/dnbuild CODE_SIGNING_ALLOWED=NO
SIM=$(xcrun simctl list devices available | grep -oE '\([0-9A-F-]{36}\)' | tr -d '()' | head -1)
xcrun simctl boot "$SIM" 2>/dev/null
xcrun simctl install "$SIM" /tmp/dnbuild/Build/Products/Debug-iphonesimulator/HanmaumDnApp.app
xcrun simctl launch --console-pty "$SIM" com.hanmaum.dn.mobile.HanmaumDnApp
xcrun simctl io "$SIM" screenshot /tmp/profile_edit_ios.png
```

Expected: app launches without a Kotlin crash; navigate to the profile edit form and screenshot it.

- [ ] **Step 4: Append the lesson**

Append to `tasks/lessons.md`:

```markdown
### Mobile DTO wire names drifted from the backend contract (zip_code, missing houseNumber)
- **Mistake**: `UpdateMyProfileRequest` (mobile) was written without checking the server DTO: it omitted `houseNumber` entirely and used `@SerialName("zip_code")` although the backend is default-Jackson camelCase. Both failed silently — the backend ignores unknown keys and treats missing keys as "keep old value" — so zip edits never saved and users embedded the house number in the street field (then saw it duplicated).
- **Rule**: When writing or changing any request DTO, open the matching DTO in `../hanmaum-dn-server` and copy the exact field names (camelCase, no naming strategy is configured server-side). For PATCH-style endpoints, add a MockEngine test asserting the serialized body's exact keys, and remember null=keep semantics: `encodeDefaults=false` must stay so nulls are omitted.
```

```bash
git add tasks/lessons.md
git commit -m "docs(lessons): DTO wire names must be copied from the server repo"
```

- [ ] **Step 5: Push and open the PR**

```bash
git push -u origin fix/profile-edit-house-number
gh pr create --base develop --title "fix(profile): house number editable in profile edit; fix zip_code wire name" --body "$(cat <<'EOF'
## Why
User feedback: the edit form has street/zip/city but no Hausnummer field; typing the house number into the street field and saving makes the house number appear twice on the profile view.

## Root cause
- `UpdateMyProfileRequest` (mobile) had no `houseNumber` field, and the edit UI never exposed one — the backend keeps `street`/`houseNumber` separate (`PATCH /members/me`, null=keep), so a street value with an embedded number duplicated the stored `houseNumber` on display.
- 🔧 Also found & fixed: the same DTO misnamed `zipCode` as `zip_code`; the backend is default-Jackson camelCase, so **zip-code edits silently never saved**.

## What
- ✅ `UpdateMyProfileRequest`: + `houseNumber`, `zip_code` → `zipCode` (composeApp/.../member/data/model/UpdateMyProfileRequest.kt)
- ✅ `MemberRepository(+Impl).updateMyProfile`: + `houseNumber` param
- ✅ `ProfileUiState.Success.editHouseNumber`, `ProfileViewModel.updateHouseNumber/cancelEditing/saveProfile`
- ✅ Edit form: HAUSNUMMER field between street and zip (existing `labelHouseNumber` strings, all 3 locales); street placeholder now street-name-only
- ✅ Tests: `MemberRepositoryImplTest` (MockEngine, asserts exact PATCH body keys incl. no `zip_code`), `ProfileViewModelTest` (StandardTestDispatcher + hand-written fakes)

## How tested
- `./gradlew :composeApp:testDevDebugUnitTest` ✅
- `./gradlew lint` — 3 pre-existing geofence errors only (known baseline) ✅
- `iosSimulatorArm64Test` with DEVELOPER_DIR ✅
- Manual: Android emulator vs dev backend (edit→save→no duplication; zip persists), iOS simulator launch — screenshots below

## 📋 Notes
- Existing rows where the house number was already saved inside `street` are not migrated; users fix it with one street edit.
- Blank fields still mean "keep old value" (backend PATCH semantics) — clearing a field from the app remains impossible for all profile fields; pre-existing, out of scope.
EOF
)"
```

Then attach the Android + iOS screenshots to the PR and merge only via the watch-then-merge chain:

```bash
gh pr checks <n> --watch --fail-fast && gh pr merge <n> --squash --delete-branch
```

---

## Explicitly out of scope

- **Profile photo from gallery → S3 upload** (the other feedback item): a new feature, blocked on a backend that doesn't exist yet, and it overlaps with the S3 work in `docs/superpowers/specs/2026-07-02-album-s3-migration-and-grid-redesign.md`. Needs its own brainstorm → spec → plan once the backend upload contract exists.
- Clearing address fields (null-vs-blank PATCH semantics) — pre-existing limitation for all profile fields.
- Migrating already-corrupted `street` values server-side.
