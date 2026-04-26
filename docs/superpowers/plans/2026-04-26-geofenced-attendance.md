# Geofenced Attendance Notification — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fire a local notification when the user enters a 100 m radius around the church during an active attendance time window, opening the Home screen on tap.

**Architecture:** Native OS geofencing (`GeofencingClient` on Android, `CLLocationManager` on iOS) behind a shared `GeofenceManager` interface. A shared `GeofenceCoordinator` fetches the church location from the server, checks the attendance time window, and triggers a platform `NotificationService`. Platform bindings are wired via a `platformModule` (`expect val`) loaded alongside the existing `appModule`.

**Tech Stack:** Ktor (existing), Koin 4.0 (existing), `kotlinx-datetime` (existing), `com.google.android.gms:play-services-location:21.3.0` (new), `CLLocationManager` / `UNUserNotificationCenter` (iOS, no new deps)

---

## File Map

### Create

| File | Purpose |
|---|---|
| `commonMain/…/features/geofence/domain/model/ChurchLocation.kt` | Domain model |
| `commonMain/…/features/geofence/domain/repository/ChurchLocationRepository.kt` | Repository interface |
| `commonMain/…/features/geofence/data/model/ChurchLocationResponse.kt` | Serializable DTO |
| `commonMain/…/features/geofence/data/repository/ChurchLocationRepositoryImpl.kt` | Ktor implementation |
| `commonMain/…/core/geofence/GeofenceManager.kt` | Platform interface |
| `commonMain/…/core/notification/NotificationService.kt` | Platform interface |
| `commonMain/…/features/geofence/domain/GeofenceCoordinator.kt` | Shared coordination logic |
| `commonMain/…/di/PlatformModule.kt` | `expect val platformModule` |
| `androidMain/…/core/geofence/GeofenceManager.android.kt` | `AndroidGeofenceManager` |
| `androidMain/…/core/geofence/GeofenceBroadcastReceiver.kt` | OS wake-up receiver |
| `androidMain/…/core/notification/NotificationService.android.kt` | `AndroidNotificationService` |
| `androidMain/…/di/PlatformModule.android.kt` | `actual val platformModule` |
| `iosMain/…/core/geofence/GeofenceManager.ios.kt` | `IosGeofenceManager` |
| `iosMain/…/core/notification/NotificationService.ios.kt` | `IosNotificationService` |
| `iosMain/…/di/PlatformModule.ios.kt` | `actual val platformModule` |
| `commonTest/…/features/geofence/data/repository/ChurchLocationRepositoryImplTest.kt` | Repo tests |
| `commonTest/…/features/geofence/domain/GeofenceCoordinatorTest.kt` | Coordinator tests |

All paths share the prefix `composeApp/src/<sourceSet>/kotlin/com/hanmaum/dn/mobile/`.

### Modify

| File | Change |
|---|---|
| `commonMain/…/di/KoinInit.kt` | Load `platformModule` alongside `appModule` |
| `commonMain/…/di/AppModule.kt` | Bind `ChurchLocationRepository`, `GeofenceCoordinator` |
| `commonMain/…/features/pending/presentation/SplashViewModel.kt` | Inject + call `GeofenceCoordinator.initialize()` on ACTIVE |
| `commonMain/…/features/announcement/presentation/HomeScreen.kt` | Show geofence rationale card |
| `androidMain/AndroidManifest.xml` | Location + notification permissions, receiver declaration |
| `gradle/libs.versions.toml` | Add `play-services-location` |
| `composeApp/build.gradle.kts` | Add `play-services-location` to `androidMain.dependencies` |
| `iosApp/iosApp/Info.plist` | Add location + notification usage strings |

---

## Task 1: ChurchLocation domain model + repository interface

**Files:**
- Create: `commonMain/…/features/geofence/domain/model/ChurchLocation.kt`
- Create: `commonMain/…/features/geofence/domain/repository/ChurchLocationRepository.kt`

- [ ] **Step 1: Create ChurchLocation**

```kotlin
// commonMain/…/features/geofence/domain/model/ChurchLocation.kt
package com.hanmaum.dn.mobile.features.geofence.domain.model

data class ChurchLocation(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
)
```

- [ ] **Step 2: Create ChurchLocationRepository**

```kotlin
// commonMain/…/features/geofence/domain/repository/ChurchLocationRepository.kt
package com.hanmaum.dn.mobile.features.geofence.domain.repository

import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation

interface ChurchLocationRepository {
    suspend fun getChurchLocation(): Result<ChurchLocation>
}
```

- [ ] **Step 3: Build to verify it compiles**

```bash
./gradlew :composeApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/geofence/
git commit -m "feat(geofence): add ChurchLocation domain model and repository interface"
```

---

## Task 2: ChurchLocationRepositoryImpl + tests

**Files:**
- Create: `commonMain/…/features/geofence/data/model/ChurchLocationResponse.kt`
- Create: `commonMain/…/features/geofence/data/repository/ChurchLocationRepositoryImpl.kt`
- Create: `commonTest/…/features/geofence/data/repository/ChurchLocationRepositoryImplTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// commonTest/…/features/geofence/data/repository/ChurchLocationRepositoryImplTest.kt
package com.hanmaum.dn.mobile.features.geofence.data.repository

import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val testJson = Json { ignoreUnknownKeys = true }

private fun mockClient(responseJson: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient =
    HttpClient(MockEngine {
        respond(
            content = responseJson,
            status = status,
            headers = headersOf("Content-Type", ContentType.Application.Json.toString())
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

class ChurchLocationRepositoryImplTest {

    private val validJson = """{"latitude":37.1234,"longitude":127.5678,"radiusMeters":100.0}"""

    @Test
    fun getChurchLocation_returnsMappedLocation() = runTest {
        val repo = ChurchLocationRepositoryImpl(mockClient(validJson))
        val result = repo.getChurchLocation()

        assertTrue(result.isSuccess)
        assertEquals(ChurchLocation(37.1234, 127.5678, 100.0), result.getOrThrow())
    }

    @Test
    fun getChurchLocation_returnsFailureOnServerError() = runTest {
        val repo = ChurchLocationRepositoryImpl(mockClient("{}", HttpStatusCode.InternalServerError))
        val result = repo.getChurchLocation()

        assertTrue(result.isFailure)
    }

    @Test
    fun getChurchLocation_requestsCorrectPath() = runTest {
        var capturedPath = ""
        val client = HttpClient(MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                content = validJson,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString())
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

        ChurchLocationRepositoryImpl(client).getChurchLocation()
        assertEquals("/church/location", capturedPath)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.ChurchLocationRepositoryImplTest"
```
Expected: FAIL — `ChurchLocationRepositoryImpl` not found

- [ ] **Step 3: Create DTO**

```kotlin
// commonMain/…/features/geofence/data/model/ChurchLocationResponse.kt
package com.hanmaum.dn.mobile.features.geofence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChurchLocationResponse(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
)
```

- [ ] **Step 4: Create repository implementation**

```kotlin
// commonMain/…/features/geofence/data/repository/ChurchLocationRepositoryImpl.kt
package com.hanmaum.dn.mobile.features.geofence.data.repository

import com.hanmaum.dn.mobile.features.geofence.data.model.ChurchLocationResponse
import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import com.hanmaum.dn.mobile.features.geofence.domain.repository.ChurchLocationRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

class ChurchLocationRepositoryImpl(
    private val client: HttpClient,
) : ChurchLocationRepository {

    override suspend fun getChurchLocation(): Result<ChurchLocation> = runCatching {
        val response = client.get("church/location")
        check(response.status == HttpStatusCode.OK) { "Unexpected status: ${response.status}" }
        response.body<ChurchLocationResponse>().toDomain()
    }

    private fun ChurchLocationResponse.toDomain() = ChurchLocation(
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
    )
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.ChurchLocationRepositoryImplTest"
```
Expected: 3 tests pass

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/geofence/data/
git add composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/geofence/
git commit -m "feat(geofence): add ChurchLocationRepositoryImpl with tests"
```

---

## Task 3: GeofenceManager + NotificationService interfaces

**Files:**
- Create: `commonMain/…/core/geofence/GeofenceManager.kt`
- Create: `commonMain/…/core/notification/NotificationService.kt`

- [ ] **Step 1: Create GeofenceManager interface**

```kotlin
// commonMain/…/core/geofence/GeofenceManager.kt
package com.hanmaum.dn.mobile.core.geofence

import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation

interface GeofenceManager {
    /** Returns true if ACCESS_FINE_LOCATION (Android) or whenInUse (iOS) is granted. */
    fun isLocationPermissionGranted(): Boolean

    /**
     * Registers the geofence with the OS.
     * [onEnter] is invoked (on any thread) when the user enters the radius.
     * Safe to call multiple times — re-registers if already active.
     */
    fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit)

    fun stopMonitoring()
}
```

- [ ] **Step 2: Create NotificationService interface**

```kotlin
// commonMain/…/core/notification/NotificationService.kt
package com.hanmaum.dn.mobile.core.notification

interface NotificationService {
    /** Returns true if the user has granted notification permission. */
    fun isNotificationPermissionGranted(): Boolean

    /** Posts a local notification prompting the user to check in. */
    fun showAttendanceNotification()
}
```

- [ ] **Step 3: Build to verify it compiles**

```bash
./gradlew :composeApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/geofence/
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/notification/
git commit -m "feat(geofence): add GeofenceManager and NotificationService interfaces"
```

---

## Task 4: GeofenceCoordinator + unit tests

**Files:**
- Create: `commonMain/…/features/geofence/domain/GeofenceCoordinator.kt`
- Create: `commonTest/…/features/geofence/domain/GeofenceCoordinatorTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// commonTest/…/features/geofence/domain/GeofenceCoordinatorTest.kt
package com.hanmaum.dn.mobile.features.geofence.domain

import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository
import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import com.hanmaum.dn.mobile.features.geofence.domain.repository.ChurchLocationRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// ── Fakes ────────────────────────────────────────────────────────────────────

private class FakeChurchLocationRepository(
    private val result: Result<ChurchLocation> =
        Result.success(ChurchLocation(37.0, 127.0, 100.0))
) : ChurchLocationRepository {
    override suspend fun getChurchLocation() = result
}

private class FakeGeofenceManager(
    private val permissionGranted: Boolean = true
) : GeofenceManager {
    var capturedOnEnter: (() -> Unit)? = null
    var isMonitoring = false

    override fun isLocationPermissionGranted() = permissionGranted
    override fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit) {
        capturedOnEnter = onEnter
        isMonitoring = true
    }
    override fun stopMonitoring() { isMonitoring = false }
}

private class FakeNotificationService : NotificationService {
    var notificationCount = 0
    override fun isNotificationPermissionGranted() = true
    override fun showAttendanceNotification() { notificationCount++ }
}

private class FakeAttendanceRepository(
    private val definitions: List<AttendanceDefinition> = emptyList()
) : AttendanceRepository {
    override suspend fun getActiveDefinitions() = Result.success(definitions)
    override suspend fun checkIn() = Result.success(Unit)
}

private fun todayName(): String =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek.name

// ── Tests ─────────────────────────────────────────────────────────────────────

class GeofenceCoordinatorTest {

    @Test
    fun initialize_registersGeofenceWhenPermissionGranted() = runTest {
        val fakeGeofence = FakeGeofenceManager(permissionGranted = true)
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(),
            fakeGeofence, FakeNotificationService(), scope = this
        )

        coordinator.initialize()

        assertTrue(fakeGeofence.isMonitoring)
    }

    @Test
    fun initialize_doesNotRegisterWhenPermissionDenied() = runTest {
        val fakeGeofence = FakeGeofenceManager(permissionGranted = false)
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(),
            fakeGeofence, FakeNotificationService(), scope = this
        )

        coordinator.initialize()

        assertFalse(fakeGeofence.isMonitoring)
    }

    @Test
    fun initialize_doesNotRegisterWhenLocationFetchFails() = runTest {
        val fakeGeofence = FakeGeofenceManager()
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(Result.failure(Exception("network error"))),
            FakeAttendanceRepository(), fakeGeofence, FakeNotificationService(), scope = this
        )

        coordinator.initialize() // must not throw

        assertFalse(fakeGeofence.isMonitoring)
    }

    @Test
    fun initialize_isNoOpWhenCalledTwice() = runTest {
        var startCallCount = 0
        val countingGeofence = object : GeofenceManager {
            override fun isLocationPermissionGranted() = true
            override fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit) { startCallCount++ }
            override fun stopMonitoring() {}
        }
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(),
            countingGeofence, FakeNotificationService(), scope = this
        )

        coordinator.initialize()
        coordinator.initialize()

        assertEquals(1, startCallCount)
    }

    @Test
    fun notifyEntry_firesNotificationWhenInsideActiveWindow() = runTest {
        val fakeNotification = FakeNotificationService()
        val fakeGeofence = FakeGeofenceManager()
        val definitions = listOf(
            AttendanceDefinition("1", "Morning Service", todayName(), "00:00:00", "23:59:59")
        )
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(definitions),
            fakeGeofence, fakeNotification, scope = this
        )

        coordinator.initialize()
        coordinator.notifyEntry()
        advanceUntilIdle()

        assertEquals(1, fakeNotification.notificationCount)
    }

    @Test
    fun notifyEntry_doesNotFireNotificationOutsideWindow() = runTest {
        val fakeNotification = FakeNotificationService()
        val fakeGeofence = FakeGeofenceManager()
        // Window: 00:00:00 – 00:00:01 (in the past, effectively never matches)
        val definitions = listOf(
            AttendanceDefinition("1", "Morning Service", todayName(), "00:00:00", "00:00:01")
        )
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(definitions),
            fakeGeofence, fakeNotification, scope = this
        )

        coordinator.initialize()
        coordinator.notifyEntry()
        advanceUntilIdle()

        assertEquals(0, fakeNotification.notificationCount)
    }

    @Test
    fun notifyEntry_doesNotFireWhenNoServiceToday() = runTest {
        val fakeNotification = FakeNotificationService()
        val fakeGeofence = FakeGeofenceManager()
        // No definition for today
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(emptyList()),
            fakeGeofence, fakeNotification, scope = this
        )

        coordinator.initialize()
        coordinator.notifyEntry()
        advanceUntilIdle()

        assertEquals(0, fakeNotification.notificationCount)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.GeofenceCoordinatorTest"
```
Expected: FAIL — `GeofenceCoordinator` not found

- [ ] **Step 3: Implement GeofenceCoordinator**

```kotlin
// commonMain/…/features/geofence/domain/GeofenceCoordinator.kt
package com.hanmaum.dn.mobile.features.geofence.domain

import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository
import com.hanmaum.dn.mobile.features.geofence.domain.repository.ChurchLocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GeofenceCoordinator(
    private val churchLocationRepository: ChurchLocationRepository,
    private val attendanceRepository: AttendanceRepository,
    private val geofenceManager: GeofenceManager,
    private val notificationService: NotificationService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) {
    private var isRegistered = false

    /**
     * Fetches the church location from the server and registers the OS geofence.
     * No-op if already registered or if location permission is not yet granted.
     * Call this after login is confirmed (e.g. from SplashViewModel when status == ACTIVE).
     */
    suspend fun initialize() {
        if (isRegistered) return
        if (!geofenceManager.isLocationPermissionGranted()) return

        val location = churchLocationRepository.getChurchLocation().getOrElse { error ->
            println("[GeofenceCoordinator] Failed to fetch church location: ${error.message}")
            return
        }

        geofenceManager.startMonitoring(location) { notifyEntry() }
        isRegistered = true
    }

    /**
     * Called by the platform geofence implementation (BroadcastReceiver on Android,
     * CLLocationManager delegate on iOS) when the user enters the monitored region.
     */
    fun notifyEntry() {
        scope.launch { onGeofenceEntered() }
    }

    private suspend fun onGeofenceEntered() {
        val definitions = attendanceRepository.getActiveDefinitions().getOrElse { return }
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val todayName = now.dayOfWeek.name
        val todayDef = definitions.firstOrNull { it.dayOfWeek == todayName } ?: return
        if (isCurrentlyInWindow(todayDef, now.hour, now.minute, now.second)) {
            notificationService.showAttendanceNotification()
        }
    }

    private fun isCurrentlyInWindow(def: AttendanceDefinition, hour: Int, minute: Int, second: Int): Boolean {
        return try {
            val startParts = def.windowStart.split(":")
            val endParts   = def.windowEnd.split(":")
            val nowSecs    = hour * 3600 + minute * 60 + second
            val startSecs  = startParts[0].toInt() * 3600 + startParts[1].toInt() * 60 + startParts[2].toInt()
            val endSecs    = endParts[0].toInt()   * 3600 + endParts[1].toInt()   * 60 + endParts[2].toInt()
            nowSecs in startSecs..endSecs
        } catch (_: Exception) { false }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.GeofenceCoordinatorTest"
```
Expected: 7 tests pass

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/geofence/domain/GeofenceCoordinator.kt
git add composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/geofence/domain/
git commit -m "feat(geofence): add GeofenceCoordinator with unit tests"
```

---

## Task 5: Platform module infrastructure (expect/actual + KoinInit)

**Files:**
- Create: `commonMain/…/di/PlatformModule.kt`
- Create: `androidMain/…/di/PlatformModule.android.kt` (stub — replaced in Task 6)
- Create: `iosMain/…/di/PlatformModule.ios.kt` (stub — replaced in Task 7)
- Modify: `commonMain/…/di/KoinInit.kt`

- [ ] **Step 1: Create expect val platformModule**

```kotlin
// commonMain/…/di/PlatformModule.kt
package com.hanmaum.dn.mobile.di

import org.koin.core.module.Module

expect val platformModule: Module
```

- [ ] **Step 2: Create Android stub actual**

```kotlin
// androidMain/…/di/PlatformModule.android.kt
package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import org.koin.dsl.module

// TODO: replaced with AndroidGeofenceManager + AndroidNotificationService in Task 6
actual val platformModule = module {
    single<GeofenceManager> {
        object : GeofenceManager {
            override fun isLocationPermissionGranted() = false
            override fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit) {}
            override fun stopMonitoring() {}
        }
    }
    single<NotificationService> {
        object : NotificationService {
            override fun isNotificationPermissionGranted() = false
            override fun showAttendanceNotification() {}
        }
    }
}
```

- [ ] **Step 3: Create iOS stub actual**

```kotlin
// iosMain/…/di/PlatformModule.ios.kt
package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import org.koin.dsl.module

// TODO: replaced with IosGeofenceManager + IosNotificationService in Task 7
actual val platformModule = module {
    single<GeofenceManager> {
        object : GeofenceManager {
            override fun isLocationPermissionGranted() = false
            override fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit) {}
            override fun stopMonitoring() {}
        }
    }
    single<NotificationService> {
        object : NotificationService {
            override fun isNotificationPermissionGranted() = false
            override fun showAttendanceNotification() {}
        }
    }
}
```

- [ ] **Step 4: Update KoinInit to load platformModule**

Replace the entire contents of `commonMain/…/di/KoinInit.kt`:

```kotlin
package com.hanmaum.dn.mobile.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(appModule, platformModule)
}
```

- [ ] **Step 5: Update AppModule.kt — add ChurchLocationRepository + GeofenceCoordinator bindings**

Add these lines to the `val appModule = module { … }` block in `commonMain/…/di/AppModule.kt` (after the existing `single<AttendanceRepository>` line):

```kotlin
// Geofence
single<ChurchLocationRepository> { ChurchLocationRepositoryImpl(get()) }
single { GeofenceCoordinator(get(), get(), get(), get()) }
```

Also add the missing imports at the top of `AppModule.kt`:

```kotlin
import com.hanmaum.dn.mobile.features.geofence.data.repository.ChurchLocationRepositoryImpl
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import com.hanmaum.dn.mobile.features.geofence.domain.repository.ChurchLocationRepository
```

- [ ] **Step 6: Build to verify it compiles**

```bash
./gradlew :composeApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/
git add composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/di/
git add composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/di/
git commit -m "feat(geofence): wire platform module infrastructure and Koin bindings"
```

---

## Task 6: Android actual — GeofenceManager + NotificationService + BroadcastReceiver

**Files:**
- Create: `androidMain/…/core/geofence/GeofenceManager.android.kt`
- Create: `androidMain/…/core/geofence/GeofenceBroadcastReceiver.kt`
- Create: `androidMain/…/core/notification/NotificationService.android.kt`
- Modify: `androidMain/…/di/PlatformModule.android.kt` (replace stubs)
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`
- Modify: `androidMain/AndroidManifest.xml`

- [ ] **Step 1: Add play-services-location to libs.versions.toml**

In `gradle/libs.versions.toml`, add to the `[versions]` block:
```toml
play-services-location = "21.3.0"
```

Add to the `[libraries]` block:
```toml
play-services-location = { module = "com.google.android.gms:play-services-location", version.ref = "play-services-location" }
```

- [ ] **Step 2: Add dependency to build.gradle.kts**

In `composeApp/build.gradle.kts`, inside `androidMain.dependencies { }`:
```kotlin
implementation(libs.play.services.location)
```

- [ ] **Step 3: Add permissions + receiver to AndroidManifest.xml**

Replace `composeApp/src/androidMain/AndroidManifest.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar"
        android:name=".DnChurchApp"
        android:usesCleartextTraffic="true">

        <activity
            android:exported="true"
            android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".core.geofence.GeofenceBroadcastReceiver"
            android:exported="false" />

    </application>

</manifest>
```

- [ ] **Step 4: Create AndroidGeofenceManager**

```kotlin
// androidMain/…/core/geofence/GeofenceManager.android.kt
package com.hanmaum.dn.mobile.core.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation

class AndroidGeofenceManager(private val context: Context) : GeofenceManager {

    private val client = LocationServices.getGeofencingClient(context)
    private var onEnterCallback: (() -> Unit)? = null

    override fun isLocationPermissionGranted(): Boolean =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    override fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit) {
        if (!isLocationPermissionGranted()) return
        onEnterCallback = onEnter

        val geofence = Geofence.Builder()
            .setRequestId("church_geofence")
            .setCircularRegion(location.latitude, location.longitude, location.radiusMeters.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        client.addGeofences(request, buildPendingIntent())
    }

    override fun stopMonitoring() {
        client.removeGeofences(buildPendingIntent())
        onEnterCallback = null
    }

    /** Called by [GeofenceBroadcastReceiver] when the OS fires the entry event. */
    fun triggerEntry() {
        onEnterCallback?.invoke()
    }

    private fun buildPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, GeofenceBroadcastReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
```

- [ ] **Step 5: Create GeofenceBroadcastReceiver**

```kotlin
// androidMain/…/core/geofence/GeofenceBroadcastReceiver.kt
package com.hanmaum.dn.mobile.core.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import org.koin.core.context.GlobalContext

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val manager = GlobalContext.get().get<GeofenceManager>() as? AndroidGeofenceManager ?: return
        manager.triggerEntry()
    }
}
```

- [ ] **Step 6: Create AndroidNotificationService**

```kotlin
// androidMain/…/core/notification/NotificationService.android.kt
package com.hanmaum.dn.mobile.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hanmaum.dn.mobile.MainActivity

private const val CHANNEL_ID = "attendance_channel"
private const val NOTIFICATION_ID = 1001

class AndroidNotificationService(private val context: Context) : NotificationService {

    init { createChannel() }

    override fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun showAttendanceNotification() {
        if (!isNotificationPermissionGranted()) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("교회에 도착하셨습니다 ⛪")
            .setContentText("출석 체크를 해주세요!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "출석 알림", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "교회 도착 시 출석 체크 알림" }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
```

- [ ] **Step 7: Replace Android stub platformModule with real implementations**

Replace the entire contents of `androidMain/…/di/PlatformModule.android.kt`:

```kotlin
// androidMain/…/di/PlatformModule.android.kt
package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.geofence.AndroidGeofenceManager
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.notification.AndroidNotificationService
import com.hanmaum.dn.mobile.core.notification.NotificationService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single<GeofenceManager> { AndroidGeofenceManager(androidContext()) }
    single<NotificationService> { AndroidNotificationService(androidContext()) }
}
```

- [ ] **Step 8: Build Android to verify it compiles**

```bash
./gradlew :composeApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/androidMain/
git add composeApp/src/androidMain/AndroidManifest.xml
git add gradle/libs.versions.toml
git add composeApp/build.gradle.kts
git commit -m "feat(geofence): implement Android GeofenceManager, BroadcastReceiver, NotificationService"
```

---

## Task 7: iOS actual — GeofenceManager + NotificationService + Info.plist

**Files:**
- Create: `iosMain/…/core/geofence/GeofenceManager.ios.kt`
- Create: `iosMain/…/core/notification/NotificationService.ios.kt`
- Modify: `iosMain/…/di/PlatformModule.ios.kt`
- Modify: `iosApp/iosApp/Info.plist`

- [ ] **Step 1: Create IosGeofenceManager**

```kotlin
// iosMain/…/core/geofence/GeofenceManager.ios.kt
package com.hanmaum.dn.mobile.core.geofence

import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import platform.CoreLocation.CLCircularRegion
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLRegion
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject

private const val REGION_ID = "church_geofence"

class IosGeofenceManager : GeofenceManager {

    private val manager = CLLocationManager()
    private var onEnterCallback: (() -> Unit)? = null

    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didEnterRegion: CLRegion) {
            if (didEnterRegion.identifier == REGION_ID) {
                onEnterCallback?.invoke()
            }
        }
    }

    init {
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyBest
    }

    override fun isLocationPermissionGranted(): Boolean {
        val status = CLLocationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedAlways ||
               status == kCLAuthorizationStatusAuthorizedWhenInUse
    }

    override fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit) {
        onEnterCallback = onEnter
        manager.requestAlwaysAuthorization()

        val region = CLCircularRegion(
            center = CLLocationCoordinate2DMake(location.latitude, location.longitude),
            radius = location.radiusMeters,
            identifier = REGION_ID,
        )
        region.notifyOnEntry = true
        region.notifyOnExit = false
        manager.startMonitoringForRegion(region)
    }

    override fun stopMonitoring() {
        manager.monitoredRegions.filterIsInstance<CLCircularRegion>()
            .filter { it.identifier == REGION_ID }
            .forEach { manager.stopMonitoringForRegion(it) }
        onEnterCallback = null
    }
}
```

- [ ] **Step 2: Create IosNotificationService**

```kotlin
// iosMain/…/core/notification/NotificationService.ios.kt
package com.hanmaum.dn.mobile.core.notification

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

class IosNotificationService : NotificationService {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override fun isNotificationPermissionGranted(): Boolean {
        var granted = false
        center.getNotificationSettingsWithCompletionHandler { settings ->
            granted = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
        }
        return granted
    }

    override fun showAttendanceNotification() {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { authorized, _ ->
            if (!authorized) return@requestAuthorizationWithOptions

            val content = UNMutableNotificationContent().apply {
                setTitle("교회에 도착하셨습니다 ⛪")
                setBody("출석 체크를 해주세요!")
            }

            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "attendance_arrival",
                content = content,
                trigger = null, // deliver immediately
            )

            center.addNotificationRequest(request) { _ -> }
        }
    }
}
```

- [ ] **Step 3: Replace iOS stub platformModule with real implementations**

Replace the entire contents of `iosMain/…/di/PlatformModule.ios.kt`:

```kotlin
// iosMain/…/di/PlatformModule.ios.kt
package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.geofence.IosGeofenceManager
import com.hanmaum.dn.mobile.core.notification.IosNotificationService
import com.hanmaum.dn.mobile.core.notification.NotificationService
import org.koin.dsl.module

actual val platformModule = module {
    single<GeofenceManager> { IosGeofenceManager() }
    single<NotificationService> { IosNotificationService() }
}
```

- [ ] **Step 4: Update Info.plist with location + notification usage strings**

Replace `iosApp/iosApp/Info.plist` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CADisableMinimumFrameDurationOnPhone</key>
    <true/>
    <key>NSLocationWhenInUseUsageDescription</key>
    <string>예배 시간에 교회에 도착하시면 출석 알림을 보내드립니다.</string>
    <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
    <string>앱이 백그라운드에 있을 때도 교회 도착 알림을 받으려면 '항상 허용'을 선택해 주세요.</string>
    <key>UIBackgroundModes</key>
    <array>
        <string>location</string>
    </array>
</dict>
</plist>
```

- [ ] **Step 5: Build to verify it compiles**

```bash
./gradlew :composeApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/iosMain/
git add iosApp/iosApp/Info.plist
git commit -m "feat(geofence): implement iOS GeofenceManager and NotificationService"
```

---

## Task 8: Permission rationale card + HomeScreen integration

**Files:**
- Modify: `commonMain/…/features/announcement/presentation/HomeScreen.kt`

The rationale card is shown when the geofence is not yet registered and location permission is not granted. It prompts the user to allow location access. The actual OS permission dialog is triggered via `rememberLauncherForActivityResult` (Android) or `IosGeofenceManager.startMonitoring` (iOS auto-requests when called).

Because the permission launcher is Android-only, we add it directly in `HomeScreen` guarded by a `@Composable expect fun GeofencePermissionRequest(onResult: (Boolean) -> Unit)`. Create these two files:

- [ ] **Step 1: Create expect composable**

```kotlin
// commonMain/…/core/geofence/GeofencePermissionRequest.kt
package com.hanmaum.dn.mobile.core.geofence

import androidx.compose.runtime.Composable

@Composable
expect fun GeofencePermissionRequest(onResult: (Boolean) -> Unit)
```

- [ ] **Step 2: Create Android actual**

```kotlin
// androidMain/…/core/geofence/GeofencePermissionRequest.android.kt
package com.hanmaum.dn.mobile.core.geofence

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun GeofencePermissionRequest(onResult: (Boolean) -> Unit) {
    val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        onResult(locationGranted)
    }

    LaunchedEffect(Unit) { launcher.launch(permissions) }
}
```

- [ ] **Step 3: Create iOS actual**

```kotlin
// iosMain/…/core/geofence/GeofencePermissionRequest.ios.kt
package com.hanmaum.dn.mobile.core.geofence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun GeofencePermissionRequest(onResult: (Boolean) -> Unit) {
    // iOS: CLLocationManager.requestAlwaysAuthorization() is called inside
    // IosGeofenceManager.startMonitoring(). Optimistically proceed here.
    LaunchedEffect(Unit) { onResult(true) }
}
```

- [ ] **Step 4: Add rationale card + permission flow to HomeScreen**

Locate `HomeScreen.kt` in `commonMain/…/features/announcement/presentation/HomeScreen.kt`. Add the following state and composable **inside the `HomeScreen` composable function**, before the first `Scaffold`:

```kotlin
// At the top of HomeScreen body, after existing state collection:
val geofenceCoordinator: GeofenceCoordinator = koinInject()
val geofenceManager: GeofenceManager = koinInject()

var showRationale by remember { mutableStateOf(false) }
var requestingPermission by remember { mutableStateOf(false) }

// Trigger geofence init on first composition (after login)
LaunchedEffect(Unit) {
    geofenceCoordinator.initialize()
    // Show rationale if location permission not yet granted
    if (!geofenceManager.isLocationPermissionGranted()) {
        showRationale = true
    }
}

val coroutineScope = rememberCoroutineScope()

if (requestingPermission) {
    GeofencePermissionRequest { granted ->
        requestingPermission = false
        showRationale = false
        if (granted) {
            coroutineScope.launch { geofenceCoordinator.initialize() }
        }
    }
}

if (showRationale) {
    GeofenceRationaleCard(
        onAllow = { requestingPermission = true },
        onDismiss = { showRationale = false }
    )
}
```

Add `GeofenceRationaleCard` as a private composable at the bottom of `HomeScreen.kt`:

```kotlin
@Composable
private fun GeofenceRationaleCard(onAllow: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "도착 알림 설정",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "예배 시간에 교회 근처에 오시면 출석 알림을 보내드립니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss) { Text("나중에") }
                Button(onClick = onAllow) { Text("권한 허용") }
            }
        }
    }
}
```

Also add the missing imports at the top of `HomeScreen.kt`:
```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.geofence.GeofencePermissionRequest
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
```

- [ ] **Step 5: Build to verify it compiles**

```bash
./gradlew :composeApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/geofence/GeofencePermissionRequest.kt
git add composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/core/geofence/
git add composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/core/geofence/
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/HomeScreen.kt
git commit -m "feat(geofence): add permission rationale card and GeofencePermissionRequest composable"
```

---

## Task 9: Wire SplashViewModel to call GeofenceCoordinator.initialize()

**Files:**
- Modify: `commonMain/…/features/pending/presentation/SplashViewModel.kt`
- Modify: `commonMain/…/di/AppModule.kt`

- [ ] **Step 1: Inject GeofenceCoordinator into SplashViewModel**

Replace `SplashViewModel.kt` with:

```kotlin
// commonMain/…/features/pending/presentation/SplashViewModel.kt
package com.hanmaum.dn.mobile.features.pending.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val tokenStorage: TokenStorage,
    private val memberRepository: MemberRepository,
    private val geofenceCoordinator: GeofenceCoordinator,
) : ViewModel() {

    private val _navigateTo = MutableStateFlow<NavRoute?>(null)
    val navigateTo = _navigateTo.asStateFlow()

    init { checkSession() }

    private fun checkSession() {
        viewModelScope.launch {
            val token = tokenStorage.getAccessToken()
            if (token.isNullOrBlank()) {
                _navigateTo.value = NavRoute.Login
                return@launch
            }

            memberRepository.getMyProfile()
                .onSuccess { member ->
                    when (member.status) {
                        MemberStatus.ACTIVE -> {
                            // Fire-and-forget: initialize geofence after confirmed login.
                            // No-op if already registered or permission not yet granted.
                            viewModelScope.launch { geofenceCoordinator.initialize() }
                            _navigateTo.value = NavRoute.Home
                        }
                        MemberStatus.PENDING -> _navigateTo.value = NavRoute.PendingApproval
                        else -> handleAuthError()
                    }
                }
                .onFailure { error ->
                    println("Auto-Login fehlgeschlagen: ${error.message}")
                    handleAuthError()
                }
        }
    }

    private fun handleAuthError() {
        viewModelScope.launch {
            tokenStorage.clear()
            _navigateTo.value = NavRoute.Login
        }
    }

    fun onNavigationHandled() {
        _navigateTo.value = null
    }
}
```

- [ ] **Step 2: Update SplashViewModel DI binding in AppModule.kt**

Find the `SplashViewModel` viewModel binding in `AppModule.kt` and update it to inject `GeofenceCoordinator`:

```kotlin
// Before:
viewModel { SplashViewModel(get(), get()) }

// After:
viewModel { SplashViewModel(get(), get(), get()) }
```

- [ ] **Step 3: Build to verify it compiles**

```bash
./gradlew :composeApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run all tests to verify nothing broke**

```bash
./gradlew :composeApp:allTests
```
Expected: all tests pass

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/pending/presentation/SplashViewModel.kt
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/AppModule.kt
git commit -m "feat(geofence): initialize GeofenceCoordinator after confirmed login in SplashViewModel"
```

---

## Manual Testing Checklist

After all tasks are complete, test on physical devices:

**Android (device with GPS):**
- [ ] Fresh install — rationale card appears on Home screen
- [ ] Tap "권한 허용" — OS dialogs appear (Fine Location, then Notifications)
- [ ] Grant "Allow all the time" from Settings — geofence registers
- [ ] Use Android Studio → Extended Controls → Location → set coordinates to church location during service hours → notification appears
- [ ] Tap notification — Home screen opens with `MorningServiceCard` visible
- [ ] Repeat with location set outside service hours → no notification

**iOS (device with GPS):**
- [ ] Fresh install — rationale card appears on Home screen
- [ ] Tap "권한 허용" — iOS prompts "When In Use", then "Always Allow"
- [ ] Use Xcode → Debug → Simulate Location → set to church coordinates during service hours → notification appears
- [ ] Tap notification — app opens to Home screen

---

## Notes for Background Permission (Android)

Android 11+ does not allow showing a dialog for `ACCESS_BACKGROUND_LOCATION`. After foreground location is granted, show a custom explanation screen and send the user to Settings manually:

```kotlin
// In the rationale card's secondary flow, after foreground granted:
val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
    data = Uri.fromParts("package", context.packageName, null)
}
context.startActivity(intent)
```

The geofence will still function with foreground-only permission while the app is open. Background wake requires the user to select "Allow all the time" in Settings.
