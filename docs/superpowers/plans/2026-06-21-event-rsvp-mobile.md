# Event RSVP (Mobile) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a member-facing Event RSVP check-in to the mobile app — a reusable bottom-sheet that auto-appears over Home during an event's RSVP window and is also reachable from an EVENT-category announcement.

**Architecture:** A new `features/events/` package in clean-architecture layers (domain / data / presentation), mirroring `features/attendance/`. A shared `EventRsvpViewModel` drives one adaptive `ModalBottomSheet`. Local `EventRsvpPreferences` suppresses already-handled events across launches (backend has no "/me"). A single `EventRsvpHost` mounted in `App.kt` triggers a refresh on app foreground.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.10, Material3, Ktor 3.3, Koin 4.1, kotlinx-serialization, kotlinx-coroutines-test, `com.russhwolf.settings`.

## Global Constraints

- **No `Co-Authored-By:` trailers** on any commit.
- **Flavored Gradle tasks only:** unit tests `:composeApp:testDevDebugUnitTest`; build `:composeApp:assembleDevDebug`. Never bare `testDebugUnitTest`/`assembleDebug`.
- **iOS link/run cannot be validated locally** (Command Line Tools only) — iOS is validated by CI `ios-check`. All new code lives in `commonMain`/`commonTest` (no `expect`/`actual`), so it compiles for both targets.
- **No 1px divider lines** between content sections — separate by surface-token shift. **No drop shadows** (tonal layering; ambient shadow for floating elements only). **Every animation uses a `spring()` spec** — no linear/ease-in-out. Pill shapes (`shape_full`) for buttons/chips. (designs/dn_app/DESIGN.md)
- **User-facing UI copy** goes through `LocalStrings` (KO/EN/DE). ViewModel-level error strings are hardcoded Korean, matching the existing `AttendanceViewModel` precedent.
- **Backend precondition:** `GET /events/rsvps/active` must include nullable `announcementId`. Until shipped, the announcement-detail CTA stays hidden (graceful); the auto-on-Home prompt is unaffected.
- **Lint gate:** `./gradlew lint` must report no NEW errors, and there must be **no literal `TODO`** in `composeApp/src` (CI greps for it and fails the build).

---

### Task 1: Data layer (domain models, DTOs, repository)

Self-contained deliverable: the app can fetch active RSVPs (parsing nullable `announcementId`) and perform a check-in that maps HTTP status to a typed result — verified by a `MockEngine` repository test.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/domain/model/EventRsvp.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/domain/model/EventRsvpCheckIn.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/domain/model/CheckInResult.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/domain/repository/EventRsvpRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/data/model/EventRsvpResponse.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/data/model/EventRsvpCheckInResponse.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/data/repository/EventRsvpRepositoryImpl.kt`
- Test: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/events/data/repository/EventRsvpRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `core/domain/model/ApiResponse` (`ApiResponse<T>(success, message, data)`); the shared Ktor `HttpClient` (base-URL injection, bearer auth). The global client has `expectSuccess = false`, so a non-2xx response returns normally and `response.status` is readable.
- Produces:
  - `data class EventRsvp(publicId: String, title: String, windowStart: String, windowEnd: String, announcementId: String?)`
  - `data class EventRsvpCheckIn(eventPublicId: String, eventTitle: String, checkedInAt: String)`
  - `sealed interface CheckInResult { data class Success(val checkIn: EventRsvpCheckIn); data object AlreadyRegistered; data object WindowClosed; data object Failed }`
  - `interface EventRsvpRepository { suspend fun getActiveRsvps(): Result<List<EventRsvp>>; suspend fun checkIn(publicId: String): CheckInResult }`

- [ ] **Step 1: Create domain models and repository interface**

`EventRsvp.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events.domain.model

/** Active RSVP as shown on the mobile sheet. Times are ISO-8601 offset datetimes. */
data class EventRsvp(
    val publicId: String,
    val title: String,
    val windowStart: String,
    val windowEnd: String,
    val announcementId: String?,
)
```

`EventRsvpCheckIn.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events.domain.model

data class EventRsvpCheckIn(
    val eventPublicId: String,
    val eventTitle: String,
    val checkedInAt: String,
)
```

`CheckInResult.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events.domain.model

/** Typed outcome of a check-in, decoupling the ViewModel from Ktor/HTTP specifics. */
sealed interface CheckInResult {
    data class Success(val checkIn: EventRsvpCheckIn) : CheckInResult
    data object AlreadyRegistered : CheckInResult // server already has this member (409)
    data object WindowClosed : CheckInResult       // outside the RSVP window (400)
    data object Failed : CheckInResult             // any other failure
}
```

`EventRsvpRepository.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events.domain.repository

import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp

interface EventRsvpRepository {
    suspend fun getActiveRsvps(): Result<List<EventRsvp>>
    suspend fun checkIn(publicId: String): CheckInResult
}
```

- [ ] **Step 2: Create the DTOs**

`EventRsvpResponse.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events.data.model

import kotlinx.serialization.Serializable

/** Wire shape of one item in `GET /events/rsvps/active`. */
@Serializable
data class EventRsvpResponse(
    val publicId: String,
    val title: String,
    val windowStart: String,
    val windowEnd: String,
    val announcementId: String? = null,
)
```

`EventRsvpCheckInResponse.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events.data.model

import kotlinx.serialization.Serializable

/** Wire shape of `POST /events/rsvps/{publicId}/check-in` data. */
@Serializable
data class EventRsvpCheckInResponse(
    val eventPublicId: String,
    val eventTitle: String,
    val checkedInAt: String,
)
```

- [ ] **Step 3: Write the failing repository test**

`EventRsvpRepositoryImplTest.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.events.data.model.EventRsvpCheckInResponse
import com.hanmaum.dn.mobile.features.events.data.model.EventRsvpResponse
import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testJson = Json { ignoreUnknownKeys = true }

private fun mockClient(
    responseJson: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpClient = HttpClient(MockEngine {
    respond(
        content = responseJson,
        status = status,
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

class EventRsvpRepositoryImplTest {

    @Test
    fun getActiveRsvps_parsesItemsIncludingNullableAnnouncementId() = runTest {
        val payload = testJson.encodeToString(
            ApiResponse(
                success = true,
                data = listOf(
                    EventRsvpResponse("e1", "여름 수련회", "2026-07-12T09:00:00+09:00", "2026-07-12T12:00:00+09:00", "ann-1"),
                    EventRsvpResponse("e2", "청년부 모임", "2026-07-12T14:00:00+09:00", "2026-07-12T16:00:00+09:00", null),
                ),
            ),
        )
        val result = EventRsvpRepositoryImpl(mockClient(payload)).getActiveRsvps()

        val list = result.getOrThrow()
        assertEquals(2, list.size)
        assertEquals("ann-1", list[0].announcementId)
        assertNull(list[1].announcementId)
    }

    @Test
    fun checkIn_201_returnsSuccess() = runTest {
        val payload = testJson.encodeToString(
            ApiResponse(success = true, data = EventRsvpCheckInResponse("e1", "여름 수련회", "2026-07-12T10:23:41+09:00")),
        )
        val result = EventRsvpRepositoryImpl(mockClient(payload, HttpStatusCode.Created)).checkIn("e1")

        assertTrue(result is CheckInResult.Success)
        assertEquals("여름 수련회", (result as CheckInResult.Success).checkIn.eventTitle)
    }

    @Test
    fun checkIn_409_returnsAlreadyRegistered() = runTest {
        val result = EventRsvpRepositoryImpl(mockClient("{}", HttpStatusCode.Conflict)).checkIn("e1")
        assertEquals(CheckInResult.AlreadyRegistered, result)
    }

    @Test
    fun checkIn_400_returnsWindowClosed() = runTest {
        val result = EventRsvpRepositoryImpl(mockClient("{}", HttpStatusCode.BadRequest)).checkIn("e1")
        assertEquals(CheckInResult.WindowClosed, result)
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.events.data.repository.EventRsvpRepositoryImplTest"`
Expected: FAIL — `EventRsvpRepositoryImpl` is unresolved.

- [ ] **Step 5: Implement the repository**

`EventRsvpRepositoryImpl.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.events.data.model.EventRsvpCheckInResponse
import com.hanmaum.dn.mobile.features.events.data.model.EventRsvpResponse
import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvpCheckIn
import com.hanmaum.dn.mobile.features.events.domain.repository.EventRsvpRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post

class EventRsvpRepositoryImpl(
    private val client: HttpClient,
) : EventRsvpRepository {

    override suspend fun getActiveRsvps(): Result<List<EventRsvp>> = runCatching {
        val response = client.get("events/rsvps/active")
        val body = response.body<ApiResponse<List<EventRsvpResponse>>>()
        body.data?.map { it.toDomain() } ?: emptyList()
    }

    // The global client uses expectSuccess = false, so non-2xx returns normally and we
    // branch on the status code. This keeps the ViewModel free of Ktor exception types.
    override suspend fun checkIn(publicId: String): CheckInResult {
        val response = client.post("events/rsvps/$publicId/check-in")
        return when (response.status.value) {
            200, 201 -> response.body<ApiResponse<EventRsvpCheckInResponse>>().data
                ?.toDomain()
                ?.let { CheckInResult.Success(it) }
                ?: CheckInResult.Failed
            409 -> CheckInResult.AlreadyRegistered
            400 -> CheckInResult.WindowClosed
            else -> CheckInResult.Failed
        }
    }

    private fun EventRsvpResponse.toDomain() =
        EventRsvp(publicId, title, windowStart, windowEnd, announcementId)

    private fun EventRsvpCheckInResponse.toDomain() =
        EventRsvpCheckIn(eventPublicId, eventTitle, checkedInAt)
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.events.data.repository.EventRsvpRepositoryImplTest"`
Expected: PASS (4 tests).

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/events
git commit -m "feat(events): RSVP data layer (models, DTOs, repository)"
```

---

### Task 2: Preferences + ViewModel

Self-contained deliverable: the brain of the feature — refresh/filter, check-in (201/409/400/other), dismiss — under deterministic unit tests. Includes the local-suppression preferences and the test fakes.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/domain/repository/EventRsvpPreferences.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/EventRsvpPreferencesImpl.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/presentation/EventRsvpUiState.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/presentation/EventRsvpViewModel.kt`
- Test: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/events/FakeEventRsvpRepository.kt`
- Test: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/events/FakeEventRsvpPreferences.kt`
- Test: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/events/EventRsvpViewModelTest.kt`

**Interfaces:**
- Consumes: `EventRsvpRepository`, `EventRsvp`, `CheckInResult` (Task 1).
- Produces:
  - `interface EventRsvpPreferences { fun isHandled(publicId: String): Boolean; fun markHandled(publicId: String) }`
  - `data class EventRsvpUiState(events: List<EventRsvp>, visible: Boolean, checkingInId: String?, checkedInIds: Set<String>, rowErrors: Map<String, String>)`
  - `class EventRsvpViewModel(repository, preferences)` exposing `uiState: StateFlow<EventRsvpUiState>` and `refresh()`, `checkIn(publicId)`, `dismiss(publicId)`, `dismissAll()`.

- [ ] **Step 1: Create the preferences interface, impl, and UI state**

`EventRsvpPreferences.kt`:
```kotlin
package com.hanmaum.dn.mobile.core.domain.repository

/**
 * Records which event RSVPs the member has already handled (checked in OR dismissed),
 * so the sheet does not re-prompt for them. The backend exposes no "did I RSVP?" query,
 * so this local record is the only suppression source across app launches. Events are
 * one-off, so the key is the RSVP publicId alone.
 */
interface EventRsvpPreferences {
    fun isHandled(publicId: String): Boolean
    fun markHandled(publicId: String)
}
```

`EventRsvpPreferencesImpl.kt`:
```kotlin
package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.EventRsvpPreferences
import com.russhwolf.settings.Settings

class EventRsvpPreferencesImpl(private val settings: Settings) : EventRsvpPreferences {
    override fun isHandled(publicId: String): Boolean =
        settings.getBoolean(key(publicId), false)

    override fun markHandled(publicId: String) =
        settings.putBoolean(key(publicId), true)

    private fun key(publicId: String) = "$PREFIX$publicId"

    private companion object {
        const val PREFIX = "event_rsvp_handled_"
    }
}
```

`EventRsvpUiState.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events.presentation

import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp

data class EventRsvpUiState(
    val events: List<EventRsvp> = emptyList(),     // active, not yet handled
    val visible: Boolean = false,                  // host sheet shown?
    val checkingInId: String? = null,              // publicId currently in flight
    val checkedInIds: Set<String> = emptySet(),    // rows showing "참석 완료 ✓"
    val rowErrors: Map<String, String> = emptyMap(),
)
```

- [ ] **Step 2: Create the test fakes**

`FakeEventRsvpRepository.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events

import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvpCheckIn
import com.hanmaum.dn.mobile.features.events.domain.repository.EventRsvpRepository

class FakeEventRsvpRepository : EventRsvpRepository {
    var activeResult: Result<List<EventRsvp>> = Result.success(emptyList())
    var defaultCheckIn: CheckInResult =
        CheckInResult.Success(EventRsvpCheckIn("e1", "행사", "2026-07-12T10:00:00+09:00"))
    val checkInResults: MutableMap<String, CheckInResult> = mutableMapOf()
    var checkInCallCount = 0

    override suspend fun getActiveRsvps(): Result<List<EventRsvp>> = activeResult

    override suspend fun checkIn(publicId: String): CheckInResult {
        checkInCallCount++
        return checkInResults[publicId] ?: defaultCheckIn
    }
}
```

`FakeEventRsvpPreferences.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events

import com.hanmaum.dn.mobile.core.domain.repository.EventRsvpPreferences

class FakeEventRsvpPreferences : EventRsvpPreferences {
    val handled = mutableSetOf<String>()
    override fun isHandled(publicId: String): Boolean = publicId in handled
    override fun markHandled(publicId: String) { handled += publicId }
}
```

- [ ] **Step 3: Write the failing ViewModel test**

`EventRsvpViewModelTest.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events

import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp
import com.hanmaum.dn.mobile.features.events.presentation.EventRsvpViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EventRsvpViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeEventRsvpRepository
    private lateinit var prefs: FakeEventRsvpPreferences

    private fun event(id: String, ann: String? = null) =
        EventRsvp(id, "행사 $id", "2026-07-12T09:00:00+09:00", "2026-07-12T12:00:00+09:00", ann)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeEventRsvpRepository()
        prefs = FakeEventRsvpPreferences()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = EventRsvpViewModel(repo, prefs)

    @Test
    fun refresh_filtersHandledEventsAndShowsSheet() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1"), event("e2")))
        prefs.markHandled("e1")

        val viewModel = vm()
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("e2"), state.events.map { it.publicId })
        assertTrue(state.visible)
    }

    @Test
    fun refresh_hidesSheetWhenNothingPending() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1")))
        prefs.markHandled("e1")

        val viewModel = vm()
        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.visible)
    }

    @Test
    fun checkIn_success_persistsAndMarksRowCheckedIn() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1")))
        repo.checkInResults["e1"] = CheckInResult.Success(
            com.hanmaum.dn.mobile.features.events.domain.model.EventRsvpCheckIn("e1", "행사 e1", "2026-07-12T10:00:00+09:00"),
        )
        val viewModel = vm()
        viewModel.refresh(); advanceUntilIdle()

        viewModel.checkIn("e1"); advanceUntilIdle()

        assertTrue("e1" in viewModel.uiState.value.checkedInIds)
        assertTrue(prefs.isHandled("e1"))
    }

    @Test
    fun checkIn_alreadyRegistered_treatedAsSuccess() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1")))
        repo.checkInResults["e1"] = CheckInResult.AlreadyRegistered
        val viewModel = vm()
        viewModel.refresh(); advanceUntilIdle()

        viewModel.checkIn("e1"); advanceUntilIdle()

        assertTrue("e1" in viewModel.uiState.value.checkedInIds)
        assertTrue(prefs.isHandled("e1"))
    }

    @Test
    fun checkIn_windowClosed_setsRowErrorAndRefreshes() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1")))
        repo.checkInResults["e1"] = CheckInResult.WindowClosed
        val viewModel = vm()
        viewModel.refresh(); advanceUntilIdle()

        // After the failed attempt, refresh() reloads — simulate the now-closed event dropping off.
        repo.activeResult = Result.success(emptyList())
        viewModel.checkIn("e1"); advanceUntilIdle()

        assertFalse(prefs.isHandled("e1"))
        assertFalse(viewModel.uiState.value.visible) // refresh cleared the empty list
    }

    @Test
    fun checkIn_ignoresSecondTapWhileInFlight() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1")))
        val viewModel = vm()
        viewModel.refresh(); advanceUntilIdle()

        viewModel.checkIn("e1") // sets checkingInId synchronously, schedules the coroutine
        viewModel.checkIn("e1") // guard must reject this one
        advanceUntilIdle()

        assertEquals(1, repo.checkInCallCount)
    }

    @Test
    fun dismissAll_marksHandledAndHides() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1"), event("e2")))
        val viewModel = vm()
        viewModel.refresh(); advanceUntilIdle()

        viewModel.dismissAll()

        assertFalse(viewModel.uiState.value.visible)
        assertTrue(prefs.isHandled("e1"))
        assertTrue(prefs.isHandled("e2"))
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.events.EventRsvpViewModelTest"`
Expected: FAIL — `EventRsvpViewModel` is unresolved.

- [ ] **Step 5: Implement the ViewModel**

`EventRsvpViewModel.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.repository.EventRsvpPreferences
import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import com.hanmaum.dn.mobile.features.events.domain.repository.EventRsvpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EventRsvpViewModel(
    private val repository: EventRsvpRepository,
    private val preferences: EventRsvpPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventRsvpUiState())
    val uiState: StateFlow<EventRsvpUiState> = _uiState.asStateFlow()

    /** Loads active RSVPs and shows the sheet for any the member has not yet handled. */
    fun refresh() {
        viewModelScope.launch {
            repository.getActiveRsvps().fold(
                onSuccess = { list ->
                    val pending = list.filterNot { preferences.isHandled(it.publicId) }
                    _uiState.update {
                        it.copy(
                            events = pending,
                            visible = pending.isNotEmpty(),
                            checkingInId = null,
                            rowErrors = emptyMap(),
                        )
                    }
                },
                onFailure = { err ->
                    // Non-critical prompt: never block Home on a network error.
                    println("[EventRsvpViewModel] active load failed: ${err.message}")
                },
            )
        }
    }

    fun checkIn(publicId: String) {
        val current = _uiState.value
        if (current.checkingInId != null || publicId in current.checkedInIds) return
        _uiState.update { it.copy(checkingInId = publicId, rowErrors = it.rowErrors - publicId) }
        viewModelScope.launch {
            when (repository.checkIn(publicId)) {
                is CheckInResult.Success, CheckInResult.AlreadyRegistered -> {
                    preferences.markHandled(publicId)
                    _uiState.update { it.copy(checkingInId = null, checkedInIds = it.checkedInIds + publicId) }
                }
                CheckInResult.WindowClosed -> {
                    _uiState.update {
                        it.copy(checkingInId = null, rowErrors = it.rowErrors + (publicId to WINDOW_CLOSED_MSG))
                    }
                    refresh()
                }
                CheckInResult.Failed -> {
                    _uiState.update {
                        it.copy(checkingInId = null, rowErrors = it.rowErrors + (publicId to FAILED_MSG))
                    }
                }
            }
        }
    }

    fun dismiss(publicId: String) {
        preferences.markHandled(publicId)
        _uiState.update {
            val remaining = it.events.filterNot { e -> e.publicId == publicId }
            it.copy(events = remaining, visible = remaining.isNotEmpty())
        }
    }

    fun dismissAll() {
        _uiState.value.events.forEach { preferences.markHandled(it.publicId) }
        _uiState.update { it.copy(events = emptyList(), visible = false) }
    }

    private companion object {
        const val WINDOW_CLOSED_MSG = "지금은 참석 가능하지 않습니다. 나중에 다시 시도해주세요."
        const val FAILED_MSG = "참석 처리에 실패했습니다"
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.events.EventRsvpViewModelTest"`
Expected: PASS (7 tests).

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/presentation composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/events
git commit -m "feat(events): RSVP preferences + ViewModel with check-in/dismiss logic"
```

---

### Task 3: Sheet UI + i18n strings

Self-contained deliverable: the pure presentational `EventRsvpSheet` (adaptive 1 vs many) plus its localized copy, compiling for both targets. No unit test (the repo has no Compose UI tests); verified by build + lint.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/i18n/AppStrings.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/i18n/AppStrings.kt` (KoStrings/EnStrings/DeStrings objects — confirm their location with the next step)
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/presentation/components/EventRsvpSheet.kt`

**Interfaces:**
- Consumes: `EventRsvp`, `EventRsvpUiState` fields (Task 2), `LocalStrings`.
- Produces:
  - 7 new `AppStrings` members: `rsvpSheetTitle, rsvpMultiHeader, rsvpAttend, rsvpAttendShort, rsvpLater, rsvpDone, rsvpAnnouncementCta`.
  - `@Composable fun EventRsvpSheet(events: List<EventRsvp>, checkingInId: String?, checkedInIds: Set<String>, rowErrors: Map<String, String>, onAttend: (String) -> Unit, onDismiss: () -> Unit)`

- [ ] **Step 1: Locate the three string implementations**

Run: `grep -rl "object KoStrings\|object EnStrings\|object DeStrings" composeApp/src/commonMain`
Expected: prints the file(s) defining `KoStrings`, `EnStrings`, `DeStrings`. Open them; each implements every `AppStrings` member. You will add the 7 new members to the interface and to all three objects.

- [ ] **Step 2: Add the 7 members to the `AppStrings` interface**

In `AppStrings.kt`, inside the `interface AppStrings { ... }` body, add a grouped block (place it near the `// Attendance` section):
```kotlin
    // Event RSVP
    val rsvpSheetTitle: String
    val rsvpMultiHeader: String
    val rsvpAttend: String
    val rsvpAttendShort: String
    val rsvpLater: String
    val rsvpDone: String
    val rsvpAnnouncementCta: String
```

- [ ] **Step 3: Implement the members in all three string objects**

In `KoStrings`:
```kotlin
    override val rsvpSheetTitle = "행사 참석"
    override val rsvpMultiHeader = "참석할 행사"
    override val rsvpAttend = "참석하기"
    override val rsvpAttendShort = "참석"
    override val rsvpLater = "나중에"
    override val rsvpDone = "참석 완료"
    override val rsvpAnnouncementCta = "행사 참석하기"
```
In `EnStrings`:
```kotlin
    override val rsvpSheetTitle = "Event RSVP"
    override val rsvpMultiHeader = "Events to attend"
    override val rsvpAttend = "Attend"
    override val rsvpAttendShort = "Attend"
    override val rsvpLater = "Later"
    override val rsvpDone = "Attending"
    override val rsvpAnnouncementCta = "RSVP to this event"
```
In `DeStrings`:
```kotlin
    override val rsvpSheetTitle = "Veranstaltung"
    override val rsvpMultiHeader = "Teilnahme"
    override val rsvpAttend = "Teilnehmen"
    override val rsvpAttendShort = "Teilnehmen"
    override val rsvpLater = "Später"
    override val rsvpDone = "Zugesagt"
    override val rsvpAnnouncementCta = "An Veranstaltung teilnehmen"
```

- [ ] **Step 4: Create the `EventRsvpSheet` composable**

`EventRsvpSheet.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp

/** Formats an ISO-8601 offset window into "MM.dd HH:mm – HH:mm" by string slicing (no TZ math). */
private fun formatWindow(start: String, end: String): String {
    fun date(s: String) = if (s.length >= 10) s.substring(5, 10).replace('-', '.') else s
    fun time(s: String) = if (s.length >= 16) s.substring(11, 16) else s
    return "${date(start)} ${time(start)} – ${time(end)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventRsvpSheet(
    events: List<EventRsvp>,
    checkingInId: String?,
    checkedInIds: Set<String>,
    rowErrors: Map<String, String>,
    onAttend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (events.isEmpty()) return
    val strings = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge, // shape_large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (events.size == 1) strings.rsvpSheetTitle else strings.rsvpMultiHeader,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))

            if (events.size == 1) {
                val e = events.first()
                SingleEvent(
                    event = e,
                    isCheckingIn = checkingInId == e.publicId,
                    isCheckedIn = e.publicId in checkedInIds,
                    error = rowErrors[e.publicId],
                    onAttend = { onAttend(e.publicId) },
                )
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    events.forEach { e ->
                        EventRow(
                            event = e,
                            isCheckingIn = checkingInId == e.publicId,
                            isCheckedIn = e.publicId in checkedInIds,
                            error = rowErrors[e.publicId],
                            onAttend = { onAttend(e.publicId) },
                        )
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text(strings.rsvpLater) }
        }
    }
}

@Composable
private fun SingleEvent(
    event: EventRsvp,
    isCheckingIn: Boolean,
    isCheckedIn: Boolean,
    error: String?,
    onAttend: () -> Unit,
) {
    val strings = LocalStrings.current
    Text(event.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    Text(
        text = formatWindow(event.windowStart, event.windowEnd),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onAttend,
        enabled = !isCheckingIn && !isCheckedIn,
        shape = MaterialTheme.shapes.extraLarge, // shape_full pill
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        when {
            isCheckingIn -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            isCheckedIn -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(strings.rsvpDone)
            }
            else -> Text(strings.rsvpAttend)
        }
    }
    ErrorLine(error)
}

@Composable
private fun EventRow(
    event: EventRsvp,
    isCheckingIn: Boolean,
    isCheckedIn: Boolean,
    error: String?,
    onAttend: () -> Unit,
) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium, // shape_medium card
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = formatWindow(event.windowStart, event.windowEnd),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ErrorLine(error)
            }
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = onAttend,
                enabled = !isCheckingIn && !isCheckedIn,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                when {
                    isCheckingIn -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    isCheckedIn -> Icon(Icons.Filled.Check, contentDescription = strings.rsvpDone, modifier = Modifier.size(18.dp))
                    else -> Text(strings.rsvpAttendShort)
                }
            }
        }
    }
}

@Composable
private fun ErrorLine(error: String?) {
    AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
        Box(modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
```

- [ ] **Step 5: Verify it compiles**

Run: `./gradlew :composeApp:compileDevDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL. If `surfaceContainerLowest` is unresolved on the active Material3 version, use `surface` and add a ghost `outline_variant` border instead — but try `surfaceContainerLowest` first.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/i18n composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/presentation/components
git commit -m "feat(events): adaptive RSVP bottom sheet + i18n strings"
```

---

### Task 4: Wire-up — DI, foreground host, App mount, announcement CTA

Self-contained deliverable: the feature is live end-to-end — auto-prompt on Home foreground via DI-provided VM, plus an EVENT-announcement CTA. Verified by full unit suite + build + lint.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/AppModule.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/presentation/EventRsvpHost.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/AnnouncementDetailScreen.kt`

**Interfaces:**
- Consumes: `EventRsvpRepository`, `EventRsvpPreferences`, `EventRsvpViewModel`, `EventRsvpSheet`, `EventRsvpUiState`.
- Produces: `@Composable fun EventRsvpHost(viewModel: EventRsvpViewModel = koinViewModel())`; Koin bindings; an EVENT-only CTA in `AnnouncementDetailScreen`.

- [ ] **Step 1: Add Koin bindings**

In `AppModule.kt`, add imports near the other feature imports:
```kotlin
import com.hanmaum.dn.mobile.core.data.repository.EventRsvpPreferencesImpl
import com.hanmaum.dn.mobile.core.domain.repository.EventRsvpPreferences
import com.hanmaum.dn.mobile.features.events.data.repository.EventRsvpRepositoryImpl
import com.hanmaum.dn.mobile.features.events.domain.repository.EventRsvpRepository
import com.hanmaum.dn.mobile.features.events.presentation.EventRsvpViewModel
```
Then, after the `// Attendance` bindings block, add:
```kotlin
    // Event RSVP
    single<EventRsvpRepository> { EventRsvpRepositoryImpl(get()) }
    single<EventRsvpPreferences> { EventRsvpPreferencesImpl(Settings()) }
    viewModel { EventRsvpViewModel(get(), get()) }
```

- [ ] **Step 2: Create `EventRsvpHost`**

`EventRsvpHost.kt`:
```kotlin
package com.hanmaum.dn.mobile.features.events.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hanmaum.dn.mobile.features.events.presentation.components.EventRsvpSheet
import org.koin.compose.viewmodel.koinViewModel

/**
 * Owns the shared RSVP ViewModel and refreshes on every app foreground (ON_START),
 * which also covers the first reach of Home. Renders the auto-prompt sheet.
 */
@Composable
fun EventRsvpHost(viewModel: EventRsvpViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (state.visible) {
        EventRsvpSheet(
            events = state.events,
            checkingInId = state.checkingInId,
            checkedInIds = state.checkedInIds,
            rowErrors = state.rowErrors,
            onAttend = viewModel::checkIn,
            onDismiss = viewModel::dismissAll,
        )
    }
}
```

- [ ] **Step 3: Mount the host in `App.kt`**

Add the import alongside the other feature imports:
```kotlin
import com.hanmaum.dn.mobile.features.events.presentation.EventRsvpHost
```
In `App.kt`, the `if (showBottomBar) { FloatingPillNav(...) }` block sits inside the outer `Box`. Immediately after that block (still inside the same `Box`), add:
```kotlin
                if (showBottomBar) {
                    EventRsvpHost()
                }
```
This shows the auto-prompt only on top-level destinations (Home and the other tabbed screens), never on detail screens.

- [ ] **Step 4: Add the EVENT-only CTA to `AnnouncementDetailScreen`**

In `AnnouncementDetailScreen.kt`, add imports. **First check the existing import block** — `fillMaxWidth` and `getValue` are already imported there; do NOT re-add them (a duplicate import fails compilation). Add only the ones not already present:
```kotlin
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.features.events.presentation.EventRsvpViewModel
import com.hanmaum.dn.mobile.features.events.presentation.components.EventRsvpSheet
```
Replace the body of `AnnouncementDetailScreen(...)` so the matching RSVP and CTA-driven sheet are wired in. The existing function loads `viewModel` (the announcement detail VM) and renders `ArticleContent` on success; keep that and add a second, independent `EventRsvpViewModel` used only for the CTA:
```kotlin
@Composable
fun AnnouncementDetailScreen(
    announcementId: String,
    onBackClick: () -> Unit,
) {
    val viewModel: AnnouncementDetailViewModel = koinViewModel(
        parameters = { parametersOf(announcementId) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Independent RSVP VM: this screen is not a top-level destination, so the global
    // EventRsvpHost is not mounted here. The sheet opens only on explicit CTA tap.
    val rsvpViewModel: EventRsvpViewModel = koinViewModel()
    val rsvpState by rsvpViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { rsvpViewModel.refresh() }
    val matching = rsvpState.events.firstOrNull { it.announcementId == announcementId }
    var sheetOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state.error != null -> {
                ErrorView(msg = state.error, onRetry = { viewModel.loadAnnouncement() })
            }
            state.announcement != null -> {
                ArticleContent(
                    item = state.announcement!!,
                    onBackClick = onBackClick,
                    // EVENT announcement with a live, window-open RSVP → show the CTA.
                    showRsvpCta = state.announcement!!.category == "EVENT" && matching != null,
                    onRsvpClick = { sheetOpen = true },
                )
            }
        }

        if (sheetOpen && matching != null) {
            EventRsvpSheet(
                events = listOf(matching),
                checkingInId = rsvpState.checkingInId,
                checkedInIds = rsvpState.checkedInIds,
                rowErrors = rsvpState.rowErrors,
                onAttend = rsvpViewModel::checkIn,
                onDismiss = { sheetOpen = false },
            )
        }
    }
}
```
Then extend `ArticleContent` to accept and render the CTA. Change its signature:
```kotlin
@Composable
private fun ArticleContent(
    item: Announcement,
    onBackClick: () -> Unit,
    showRsvpCta: Boolean = false,
    onRsvpClick: () -> Unit = {},
) {
```
And inside `ArticleContent`, in the article-body `Column` just before the final `Spacer(Modifier.height(40.dp))` (after the Hashtags row), add:
```kotlin
            if (showRsvpCta) {
                val strings = LocalStrings.current
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onRsvpClick,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(strings.rsvpAnnouncementCta) }
            }
```

- [ ] **Step 5: Build the Android app**

Run: `./gradlew :composeApp:assembleDevDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Run the full unit suite + lint**

Run: `./gradlew :composeApp:testDevDebugUnitTest`
Expected: PASS (incl. the 11 new event tests).

Run: `./gradlew lint`
Expected: no NEW errors beyond the 3 pre-existing geofence/notification ones noted in `tasks/lessons.md`.

Run: `grep -rn "TODO" composeApp/src` — Expected: no matches introduced by this work (CI fails the build on any match).

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/AppModule.kt composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/events/presentation/EventRsvpHost.kt composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/AnnouncementDetailScreen.kt
git commit -m "feat(events): wire RSVP host on Home foreground + EVENT announcement CTA"
```

---

## Verification (whole feature)

- [ ] `./gradlew :composeApp:testDevDebugUnitTest` green.
- [ ] `./gradlew :composeApp:assembleDevDebug` green.
- [ ] `./gradlew lint` — no new errors; no `TODO` in `composeApp/src`.
- [ ] iOS validated via CI `ios-check` on the PR (cannot run locally).
- [ ] Manual smoke (optional, simulator): with an active RSVP returned by `GET /active`, the sheet auto-shows over Home on foreground; tapping 참석하기 shows 참석 완료 and the sheet does not re-appear after dismiss/relaunch.

## Notes / Known Limitations

- The **EVENT-announcement CTA stays hidden until the backend adds `announcementId`** to `GET /events/rsvps/active` (matching is `it.announcementId == announcementId`). The auto-on-Home prompt works regardless. See `docs/superpowers/specs/2026-06-21-event-rsvp-mobile-design.md` §8.
- The success row uses `checkedInIds` to render 참석 완료; the event remains in the sheet until the member dismisses (or the next foreground refresh filters it out via persisted state). There is intentionally **no auto-close timer** in the ViewModel — keeps it deterministic.
- **No member un-RSVP** (no backend endpoint) — out of scope per spec §11.
