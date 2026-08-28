# Notifications — Push + In-App Bell (Design Spec)

**Date:** 2026-07-15
**Status:** Approved design, pending implementation plan
**Scope:** Full stack — `hanmaum-dn-server` (Spring) + this app (KMP). Admin web app is untouched (announcement creation already exists there).
**Research:** `.lazyweb/design-research/notification-center-2026-07-15/report.md` (local, gitignored)

## 1. Overview

When an announcement is created (and immediately active), every active member gets:
1. an **FCM push notification** ("새로운 소식이 있습니다!") on Android + iOS, and
2. an **in-app notification row** behind the home-screen bell.

The bell shows a numeric **unseen** badge. Tapping it navigates to a full notification
screen (no dropdown/bottom sheet — confirmed decision). Users can turn push off in
Settings; the in-app bell keeps working. The schema supports future types
(event reminders etc.) without layout or migration changes.

### Read-state model (two-tier — the core semantic)

| State | Meaning | Set when | Drives |
|---|---|---|---|
| **unseen** (`seen_at IS NULL`) | user hasn't opened the notification UI since this arrived | `POST /me/notifications/mark-seen` when the notification screen opens | bell badge count, APNs app-icon badge |
| **unread** (`read_at IS NULL`) | user hasn't opened this specific item | `POST /me/notifications/{id}/read` when the row is tapped (or read-all) | per-row dot + bold title |

Badge clears on screen open; per-row dot persists until the item is tapped. `read`
implies nothing about `seen` in code — the server sets `seen_at` alongside `read_at`
if still null when marking read.

## 2. Decisions and defaults (all confirmed with user)

| Decision | Choice |
|---|---|
| Delivery architecture | Fan-out on write: one `notifications` row per target member + device-token registry, direct FCM multicast sends |
| Push transport | FCM for BOTH platforms (FCM relays to APNs for iOS) |
| MVP trigger | Announcement create only, and only if `startAt <= now`. Future-dated announcements do NOT push in MVP (no scheduler yet) |
| Bell tap | Straight to full notification screen |
| Push toggle | Member-level server flag (`push_enabled`), one switch in SettingsScreen, applies to all the member's devices. Rows are written regardless of the flag |
| New mobile deps (approved) | `firebase-messaging` (Android, via `com.google.gms.google-services` plugin), `FirebaseMessaging` (iOS, SPM in iosApp). NO KMP wrapper library |
| New server dep (approved) | `com.google.firebase:firebase-admin` |
| Badge cap | Display `9+` above 9 |
| Notification retention | None in MVP (no pruning job) |
| Per-type push toggles | Out of scope (single master toggle) |
| App-icon badge (iOS) | APNs `badge` = member's unseen count, computed per member during fan-out |

## 3. Firebase prerequisites (manual, before coding)

One Firebase project, two app registrations:
- Android app `com.hanmaum.dn.mobile` (all flavors share this applicationId) → `google-services.json` into `composeApp/`. Safe to commit (not a secret).
- iOS app `com.hanmaum.dn.mobile.HanmaumDnApp` → `GoogleService-Info.plist` into `iosApp/iosApp/`. Safe to commit.
- Service-account JSON (Project Settings → Service accounts) → **secret**, never committed. Server reads it from env `FIREBASE_SERVICE_ACCOUNT_JSON` (the raw JSON string) — add to server deploy env + docs.
- iOS: upload the APNs auth key (.p8) to Firebase Cloud Messaging settings, and add the **Push Notifications capability** to the iosApp target (`aps-environment` entitlement — note lessons.md: entitlement changes must exist in the App Store provisioning profile; Push is auto-satisfied per lesson §63.2).

## 4. Server design (`../hanmaum-dn-server`)

Follow the existing feature layout (`features/announcements/` is the reference):
`features/notifications/{domain,repository,service,api/v1,api/v1/dto}`.

### 4.1 Schema (Flyway migration)

```sql
CREATE TABLE device_tokens (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    token VARCHAR(512) NOT NULL UNIQUE,
    platform VARCHAR(16) NOT NULL,          -- ANDROID | IOS
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_device_tokens_member ON device_tokens(member_id);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    member_id BIGINT NOT NULL REFERENCES members(id),
    type VARCHAR(32) NOT NULL,              -- ANNOUNCEMENT (enum, string-stored)
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    reference_type VARCHAR(32),             -- ANNOUNCEMENT | NULL
    reference_public_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    seen_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ
);
CREATE INDEX idx_notifications_member_created ON notifications(member_id, created_at DESC);
CREATE INDEX idx_notifications_member_unseen ON notifications(member_id) WHERE seen_at IS NULL;
```

Member table gains `push_enabled BOOLEAN NOT NULL DEFAULT TRUE` (separate migration
statement). Match column/entity conventions to `BaseEntity` exactly as
`announcements` does (check its migration for the real members FK name/type first).

### 4.2 Endpoints (all authenticated, `ApiResponse<T>` wrapped, base `/api/v1`)

Exact wire names below are THE contract — the mobile DTOs copy them verbatim
(camelCase, default Jackson; no naming strategy — see lessons.md zip_code entry).

| Method + path | Request body | `data` response |
|---|---|---|
| `PUT /me/device-tokens` | `{"token": "...", "platform": "ANDROID"}` | `null` (upsert by token; re-assign member if token exists for another member) |
| `DELETE /me/device-tokens/{token}` | — | `null` (idempotent) |
| `GET /me/notifications?page=0&size=20` | — | `{"items": [NotificationResponse...], "page": 0, "hasNext": true}` |
| `GET /me/notifications/unseen-count` | — | `{"count": 3}` |
| `POST /me/notifications/mark-seen` | — | `null` (all unseen → `seen_at = now`) |
| `POST /me/notifications/{publicId}/read` | — | `null` (sets `read_at`, and `seen_at` if null; 404 if not caller's) |
| `POST /me/notifications/read-all` | — | `null` |
| `PUT /me/notification-settings` | `{"pushEnabled": false}` | `null` |
| `GET /me/notification-settings` | — | `{"pushEnabled": true}` |

`NotificationResponse`:
```json
{
  "publicId": "uuid", "type": "ANNOUNCEMENT",
  "title": "새로운 소식이 있습니다!", "body": "여름 수련회 안내",
  "referenceType": "ANNOUNCEMENT", "referencePublicId": "uuid",
  "createdAt": "2026-07-15T12:00:00Z",
  "seenAt": null, "readAt": null
}
```
List is ordered `created_at DESC`. Page size default 20, max 50.

### 4.3 Fan-out pipeline

1. `AnnouncementService.createAnnouncement` publishes `AnnouncementCreatedEvent(announcement)`
   via `ApplicationEventPublisher` (only when `startAt <= now`).
2. `NotificationFanoutListener` — `@TransactionalEventListener(phase = AFTER_COMMIT)`,
   `@Async` — so a push/FCM failure can NEVER roll back or slow announcement creation:
   a. Load all ACTIVE members.
   b. Batch-insert one notification row per member
      (`type=ANNOUNCEMENT`, `title="새로운 소식이 있습니다!"`, `body=announcement.title`,
      `referenceType=ANNOUNCEMENT`, `referencePublicId=announcement.publicId`).
   c. For members with `push_enabled = true`, load their device tokens; compute each
      member's unseen count (post-insert); send FCM per member
      (`MulticastMessage` over the member's tokens):
      - notification block: title/body as above (FCM renders when app is backgrounded)
      - data block (all values strings): `type`, `referenceType`, `referencePublicId`, `notificationPublicId`
      - `ApnsConfig` → `Aps.badge = unseenCount`, `sound = "default"`
      - Android config: channel id `announcements`
   d. On per-token `UNREGISTERED` / `INVALID_ARGUMENT` responses, delete those token rows.
   e. Log-and-continue on any FCM exception. No retries in MVP.
3. `FirebaseApp` initialized once from `FIREBASE_SERVICE_ACCOUNT_JSON` in a `@Configuration`
   bean; if env var absent (local dev), bean logs a warning and fan-out skips step c
   (rows still written) — local dev must not require Firebase credentials.

### 4.4 Server tests

- Service test: create announcement → rows created for ACTIVE members only; no rows/push for future `startAt`; push skipped for `push_enabled=false` members (fake the FCM sender interface — wrap `FirebaseMessaging` behind a `PushSender` interface so tests never touch Firebase).
- Controller tests: each endpoint's happy path + `{publicId}/read` for another member's notification → 404. Mark-seen only touches caller's rows.

## 5. Mobile design (this repo)

### 5.1 File layout

```
features/notification/
  domain/model/Notification.kt            # + NotificationType, NotificationReference
  domain/repository/NotificationRepository.kt
  data/model/NotificationDtos.kt
  data/repository/NotificationRepositoryImpl.kt
  presentation/NotificationListViewModel.kt
  presentation/NotificationListScreen.kt
  presentation/components/NotificationRow.kt
core/push/PushManager.kt                  # expect-free common interface
androidMain .../core/push/AndroidPushManager.kt + DnFirebaseMessagingService.kt
iosMain    .../core/push/IosPushManager.kt
iosApp: AppDelegate additions + PushBridge call into Kotlin
```

### 5.2 Domain

```kotlin
enum class NotificationType { ANNOUNCEMENT, EVENT_REMINDER, UNKNOWN }
// DTO->domain mapping: unknown wire string -> UNKNOWN (never crash on new server types)

data class NotificationReference(val type: NotificationType, val publicId: String)

data class Notification(
    val publicId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val reference: NotificationReference?,   // null => row is not tappable-to-detail
    val createdAt: Instant,
    val isSeen: Boolean,
    val isRead: Boolean,
)

interface NotificationRepository {
    suspend fun getNotifications(page: Int): Result<NotificationPage>  // items + hasNext
    suspend fun getUnseenCount(): Result<Int>
    suspend fun markAllSeen(): Result<Unit>
    suspend fun markRead(publicId: String): Result<Unit>
    suspend fun markAllRead(): Result<Unit>
    suspend fun getPushEnabled(): Result<Boolean>
    suspend fun setPushEnabled(enabled: Boolean): Result<Unit>
    suspend fun registerDeviceToken(token: String, platform: String): Result<Unit>
    suspend fun deleteDeviceToken(token: String): Result<Unit>
}
```

DTOs: `@Serializable`, field names copied 1:1 from §4.2 (write the MockEngine
body-key tests BEFORE the impl — zip_code lesson). Unwrap `ApiResponse<T>` in the
repository; non-2xx → `Result.failure` (client has `expectSuccess = false`).
Relative URLs only (shared client injects backend base URL).

### 5.3 Push plumbing

```kotlin
// commonMain core/push/PushManager.kt — implemented per platform, bound in PlatformModule.{android,ios}.kt
interface PushManager {
    suspend fun currentToken(): String?              // null if unavailable/not yet granted
    val tokenRefreshes: Flow<String>                 // FCM token rotation
    val notificationTaps: Flow<PushTapPayload>       // taps incl. cold-start replay
    suspend fun requestPermission(): Boolean         // OS prompt; true if granted
    fun isPermissionGranted(): Boolean
}
data class PushTapPayload(val type: String?, val referenceType: String?,
                          val referencePublicId: String?, val notificationPublicId: String?)
```

- **Android** (`DnFirebaseMessagingService`): `onNewToken` → emit to `tokenRefreshes`.
  `onMessageReceived` (foreground only — background messages with a notification
  block are rendered by FCM automatically) → show local notification on a new
  channel `announcements` (name via strings; reuse `AndroidNotificationService`
  channel-creation pattern) with the data payload copied into the tap intent.
  `MainActivity.onNewIntent`/`onCreate` extracts push extras → forwards to
  `notificationTaps` (replay-1 buffer so cold-start taps survive until Home collects).
- **iOS**: Swift `AppDelegate`: `FirebaseApp.configure()`, register for remote
  notifications, `MessagingDelegate.didReceiveRegistrationToken` and
  `UNUserNotificationCenterDelegate.didReceive(response:)` both call small exported
  Kotlin bridge functions (no default params, remember `init*` → `doInit*` rename;
  add explicit wrappers in `KoinHelper.kt`-style file). Foreground presentation:
  `.banner, .sound, .badge`. `IosPushManager` backs the same flows; permission via
  `UNUserNotificationCenter`.
- **Token lifecycle**: after Splash routes an ACTIVE member to Home →
  `pushManager.currentToken()?.let { repo.registerDeviceToken(it, platform) }`
  (fire-and-forget, log failure). Collect `tokenRefreshes` → re-register.
  Logout flow → `deleteDeviceToken(token)` best-effort before clearing auth.
- **Deep link**: Home collects `notificationTaps`; payload with
  `referenceType == "ANNOUNCEMENT"` → `navController.navigate(AnnouncementDetailRoute(referencePublicId))`
  + `markRead(notificationPublicId)`. Unknown reference → navigate to
  `NotificationListRoute` instead. Uses existing `@Serializable` routes ONLY.

### 5.4 Presentation

- `HomeViewModel`: add `unseenCount: Int` to `HomeUiState`; fetch on init and on
  every return to Home (lifecycle-resume pattern already used on Home). That is the
  ONLY badge refresh trigger in MVP — no live refresh on foreground push arrival
  (a foreground banner tap deep-links to detail anyway; next resume corrects the count).
- `NotificationListViewModel`:
  `NotificationListUiState(items: List<Notification>, isLoading, isLoadingMore, error: String?, hasNext, allRead: Boolean)`.
  On start: page 0 load, then `markAllSeen()` (fire-and-forget). `onItemClick`:
  optimistic `isRead=true` in state, `markRead` fire-and-forget, emit nav intent to
  announcement detail (reference null → no-op). `onReadAll`: optimistic all-read +
  `markAllRead()`. Infinite scroll via `hasNext`.
- `SettingsScreen`: "푸시 알림" `Switch` bound to `getPushEnabled()`/`setPushEnabled`
  through `SettingsViewModel`; helper text row shown when
  `!pushManager.isPermissionGranted()`.
- Priming card on Home (first ACTIVE session only, dismissal persisted in
  multiplatform-settings key `push_prompt_dismissed`): title/body/CTA per §6; CTA →
  `pushManager.requestPermission()`; either outcome dismisses the card.
- Navigation: `@Serializable object NotificationListRoute` in `Routes.kt`;
  `composable<NotificationListRoute>` in `App.kt`; bell `IconButton` navigates to it.
  NOT a tab — pushed detail-style screen (chevron back + swipe back both pop).

### 5.5 UI spec (DESIGN.md tokens only — zero literals in screen files)

- **Bell badge**: `BadgedBox`, badge `primary` bg / `on_primary` text, text `label`
  style, cap `9+`; appear/disappear = spring scale (dampingRatio 0.6, stiffness 400).
- **Screen**: top bar = 44dp chevron-left + title "알림" (`headline`) + "모두 읽음"
  text button (`title_medium`, `primary`; disabled 38% opacity when `allRead`).
- Group headers 오늘/어제/이전 (`label` style UPPERCASE, `muted`) — computed from
  `createdAt` in device TZ. Rows: `surface_container_lowest` cards, `shape_medium`,
  14dp inner padding, `space_sm` gaps, NO dividers.
- Row: 40dp leading circle — ANNOUNCEMENT: megaphone (`Icons.Default.Campaign`) on
  `primary` @15% alpha, icon `primary`; UNKNOWN: bell on `muted` @15%;
  EVENT_REMINDER (future, mapping defined now): calendar icon on `secondary` tint. Title
  (`title_medium`; unread weight 700 `on_surface`, read weight 500 `on_surface_variant`),
  body preview 1 line ellipsized (`body_medium`), relative time (`body_medium`,
  `muted`). Trailing 8dp `primary` dot when unread. Press = scale 0.97 spring.
- List entry stagger: alpha+8dp translate, spring, 40ms/item, cap 5.
- Empty state: 72dp bell in tinted circle + "알림이 오면 여기에 표시됩니다" (`body_large`,
  `on_surface_variant`), centered.
- Both light and dark verified. Korean line-height ≥1.6.

### 5.6 Strings (AppStrings + Ko/En/De — every one, compile-enforced)

`notificationsTitle` (알림), `notificationsEmpty`, `notificationsReadAll` (모두 읽음),
`notificationsToday`/`Yesterday`/`Earlier` (오늘/어제/이전),
`notificationTimeMinutesAgo`/`HoursAgo`/`DaysAgo` (format args),
`pushPrimingTitle`/`Body`/`Enable`/`Later`, `settingsPushToggle` (푸시 알림),
`settingsPushPermissionHint`, `notificationChannelAnnouncements` (Android channel name).
(Home bell `contentDescription` already exists: `strings.notifications`.)

### 5.7 Koin

`AppModule.kt`: `single<NotificationRepository> { NotificationRepositoryImpl(get()) }`,
`viewModel { NotificationListViewModel(get()) }` (+ inject repo into Home/Settings VMs).
`PlatformModule.android.kt` / `.ios.kt`: `single<PushManager> { ... }`.

## 6. Error handling

- Unseen-count failure → badge hidden, no user-visible error.
- List load failure → error state + retry button; pagination failure → toast-less
  inline retry row.
- `markAllSeen`/`markRead`/`markAllRead` are optimistic fire-and-forget; failure =
  state reverts naturally on next server load. No dialogs.
- Token registration failure → log only; retried next app start.
- ViewModel-internal error strings: hardcoded Korean (repo precedent).

## 7. Testing plan (mobile)

Hand-written fakes; `StandardTestDispatcher`; test names = letters/digits/spaces only.

1. `NotificationListViewModelTest`: load populates + calls markAllSeen; item click
   marks read optimistically + emits nav intent; read-all flips all + disables action;
   pagination appends; error surfaces.
2. `HomeViewModel` badge: unseen count fetched; failure leaves count 0.
3. `NotificationRepositoryImplTest` (MockEngine): exact request paths/methods; exact
   serialized body keys for `PUT /me/device-tokens` and `PUT /me/notification-settings`;
   `ApiResponse` unwrap; unknown `type` string maps to `UNKNOWN`; non-2xx → failure.
4. Push payload parsing (data map → `PushTapPayload` → nav decision) in commonTest.

## 8. Verification (definition of done)

- `./gradlew :composeApp:testDevDebugUnitTest` ✓, TODO grep ✓, `./gradlew lint`
  (3-error baseline) ✓, `DEVELOPER_DIR=... :composeApp:iosSimulatorArm64Test` ✓,
  xcodebuild Swift interop gate ✓ (AppDelegate changed → mandatory).
- E2E on Android emulator against real backend: create announcement → push received
  (background + foreground) → badge increments → screen opens/marks seen → tap routes
  to detail + dot clears → settings toggle off → next announcement: no push, row still
  appears. iOS simulator: full UI flow (simulator gets no real APNs push — verify push
  path on a TestFlight device build; note this residual risk in the PR).
- Screenshots light+dark, both platforms, for the PR. `../dn-app/MVP.md` row updated.

## 9. Out of scope (explicitly deferred)

- Event reminders / scheduled sends (needs a scheduler; schema already fits).
- Per-type push preferences; notification pruning/retention; admin-triggered custom
  pushes; app-icon badge sync beyond push-time `badge` field; Android app-icon badges.
- Push for future-dated announcements at their `startAt`.
