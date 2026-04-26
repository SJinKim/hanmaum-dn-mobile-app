# Geofenced Attendance Notification — Design Spec

**Date:** 2026-04-26
**Feature:** Notify the user to check in when they arrive near the church during an active attendance window.

---

## Overview

When a user enters a 100m radius around the church during an active attendance time window, the app fires a local notification prompting them to check in. Tapping the notification opens the Home screen where `MorningServiceCard` is already visible. The geofence works in the background and foreground using native OS APIs.

---

## Architecture

### New shared components (commonMain)

**`features/geofence/domain/model/ChurchLocation.kt`**
```
data class ChurchLocation(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double   // default 100.0, server-controlled
)
```

**`features/geofence/domain/repository/ChurchLocationRepository.kt`**
```
interface ChurchLocationRepository {
    suspend fun getChurchLocation(): Result<ChurchLocation>
}
```

**`features/geofence/data/model/ChurchLocationResponse.kt`**
Serializable DTO mirroring `ChurchLocation`. Mapped to domain model in `ChurchLocationRepositoryImpl`.

**`features/geofence/data/repository/ChurchLocationRepositoryImpl.kt`**
Ktor `GET /api/v1/church/location` → `ChurchLocation`.

**`features/geofence/domain/GeofenceCoordinator.kt`**
The single shared coordination class. Responsibilities:
1. Fetch `ChurchLocation` from `ChurchLocationRepository`
2. Request permissions via `NotificationService` and `GeofenceManager`
3. Call `GeofenceManager.startMonitoring(location)`
4. On geofence entry: call `AttendanceRepository.getActiveDefinitions()`, check if `now()` is within any `windowStart`–`windowEnd`
5. If inside a window: call `NotificationService.showAttendanceNotification()`
6. If church location fetch fails: log and skip registration silently — retry on next app start

### New platform interfaces (expect/actual)

**`core/geofence/GeofenceManager.kt`** (expect class)
```
expect class GeofenceManager {
    fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit)
    fun stopMonitoring()
}
```

**`core/notification/NotificationService.kt`** (expect class)
```
expect class NotificationService {
    suspend fun requestPermission(): Boolean
    fun showAttendanceNotification()
}
```

### Android actuals (androidMain)

- **`actual GeofenceManager`**: Uses `GeofencingClient` (Google Play Services Fused Location). Registers a `PendingIntent` targeting a `BroadcastReceiver` so the OS wakes the app in the background.
- **`actual NotificationService`**: Uses `NotificationCompat` + `NotificationManager`. Requests `POST_NOTIFICATIONS` on Android 13+. Tapping the notification sends an `Intent` that opens `MainActivity` which navigates to `HomeRoute`.

**New AndroidManifest entries:**
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<receiver android:name=".core.geofence.GeofenceBroadcastReceiver"
          android:exported="false" />
```

### iOS actuals (iosMain)

- **`actual GeofenceManager`**: Uses `CLLocationManager` with `CLCircularRegion(center:radius:identifier:)`. Implements `locationManager(_:didEnterRegion:)` delegate method.
- **`actual NotificationService`**: Uses `UNUserNotificationCenter.requestAuthorization(options:)` and `UNUserNotificationCenter.add(_:)`.

**New Info.plist entries:**
```
NSLocationWhenInUseUsageDescription
NSLocationAlwaysAndWhenInUseUsageDescription
```

### DI wiring (AppModule.kt additions)

```kotlin
single { ChurchLocationRepositoryImpl(get()) } bind ChurchLocationRepository::class
single { GeofenceManager() }          // platform actual
single { NotificationService() }      // platform actual
single { GeofenceCoordinator(get(), get(), get(), get()) }
```

`GeofenceCoordinator.initialize()` called from `SplashViewModel` after it confirms a valid token and navigates to `HomeRoute` — not on app start. This ensures the Bearer token is available for `GET /api/v1/church/location`. It is a no-op if the geofence is already registered.

---

## Server API

**New endpoint (backend change required):**
```
GET /api/v1/church/location
Authorization: Bearer <token>

Response 200:
{
  "latitude": 37.1234,
  "longitude": 127.5678,
  "radiusMeters": 100.0
}
```

`radiusMeters` is admin-configurable server-side. Recommended range: **100–300m**. The OS minimum for reliable geofencing on both Android and iOS is 100m; values below this produce inconsistent background results.

---

## Permissions Flow

### Android (10+)

1. **In-app rationale card** — shown once after first login on Home screen:
   _"도착 알림을 보내려면 위치 권한이 필요합니다. 예배 시간에 교회 근처에 오시면 출석 알림을 보내드립니다."_
2. **System dialog — Fine Location** (`ACCESS_FINE_LOCATION`)
   - Denied → geofence not registered, feature silently disabled
3. **System dialog — Notifications** (`POST_NOTIFICATIONS`, Android 13+)
   - Denied → geofence still registered, notification is silent
4. **In-app explanation** — required before background location:
   _"백그라운드 위치 접근이 필요합니다. 설정에서 '항상 허용'을 선택해 주세요."_
5. **Opens OS Settings** (Android 11+ cannot show a dialog for background location)
   - User selects "Allow all the time" → full background geofencing
   - Skipped → foreground-only, still functional while app is open

### iOS

1. **In-app rationale card** — same copy as Android
2. **System dialog — When In Use** (`NSLocationWhenInUseUsageDescription`)
   - Denied → feature silently disabled
3. **System dialog — Always Allow** (`NSLocationAlwaysAndWhenInUseUsageDescription`)
   - iOS presents this as an upgrade prompt after `whenInUse` is granted
   - Declined → iOS re-prompts automatically after a few background uses
4. **System dialog — Notifications** (`UNUserNotificationCenter.requestAuthorization`)
   - Denied → geofence still registered, notification is silent

---

## Data Flow

```
Login confirmed (SplashViewModel navigates to HomeRoute)
  → GeofenceCoordinator.initialize()  [no-op if already registered]
  → GET /api/v1/church/location                        [ChurchLocationRepositoryImpl]
  → request permissions                                [NotificationService, GeofenceManager]
  → GeofenceManager.startMonitoring(ChurchLocation)    [OS now watches for entry]

--- time passes, app may be closed ---

User enters 100m zone
  → OS fires entry event (BroadcastReceiver / CLLocationManager delegate)
  → GeofenceCoordinator.onEnter()
  → AttendanceRepository.getActiveDefinitions()
  → is now() within windowStart..windowEnd?
      NO  → do nothing
      YES → NotificationService.showAttendanceNotification()
              "교회에 도착하셨습니다 ⛪ 출석 체크를 해주세요!"

User taps notification
  → App opens to HomeRoute
  → MorningServiceCard visible
  → User taps 출석하기
  → POST /api/v1/attendance/check-in
```

---

## Error Handling

| Scenario | Behaviour |
|---|---|
| Server unreachable on init | Skip registration, retry silently on next app start |
| Location permission denied | No geofence registered; manual check-in unaffected |
| Notification permission denied | Geofence registered; entry detected but no notification shown |
| User enters zone outside time window | No notification fired |
| Notification tapped when already checked in | Home opens; card shows "출석 완료 ✓" |
| OS evicts geofence (iOS aggressive memory) | Re-registered on next app foreground |

---

## Testing

**Unit tests (commonTest):**
- `GeofenceCoordinatorTest` — fake `GeofenceManager` and fake `AttendanceRepository`
  - Asserts notification fires when `onEnter` called inside a time window
  - Asserts notification does NOT fire outside time window
  - Asserts no crash when `ChurchLocationRepository` returns failure
- `ChurchLocationRepositoryImplTest` — Ktor mock engine (same pattern as `AnnouncementRepositoryImplTest`)
  - 200 OK → correct `ChurchLocation` mapped
  - Non-200 → `Result.failure`

**Manual device tests:**
- Background geofence wake cannot be reliably simulated; test on physical Android and iOS device
- Use Android Studio's "Extended Controls → Location" to simulate arrival
- Use Xcode's "Simulate Location" for iOS

---

## Out of Scope

- Geofence exit detection (no "you've left the church" notification)
- Multiple church locations
- User-facing toggle to disable the feature (can be added later)
- Push notifications from the server (this feature uses local notifications only)
