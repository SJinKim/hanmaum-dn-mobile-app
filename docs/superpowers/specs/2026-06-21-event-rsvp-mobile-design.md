# Event RSVP — Mobile Design Spec

**Date:** 2026-06-21
**Status:** Approved for implementation
**Scope:** `hanmaum-dn-mobile-app` (KMP / Compose Multiplatform). Backend contract per dn-server "Event RSVP" spec.

---

## 1. Overview

Church events need a lightweight RSVP check-in on mobile. During an event's RSVP
window, the app surfaces a **bottom-sheet modal** prompting the member to tap
**참석하기** (Attend). The same sheet is also reachable from an `EVENT`-category
announcement via a primary CTA. This is intentionally separate from the recurring
`attendance` feature — events are one-off and date-specific.

Member-facing flow only. Admin/group-leader RSVP management (create/update/
deactivate/attendee-list) is **out of scope** for the mobile app.

---

## 2. Decisions (locked during brainstorming)

| # | Decision | Choice |
|---|----------|--------|
| 1 | Announcement → RSVP linkage | Consume `announcementId` from `GET /events/rsvps/active` (**backend dependency**, see §8). |
| 2 | Surface | Reusable `ModalBottomSheet`, auto-shown over Home. |
| 3 | Multiple active events | **Adaptive single sheet**: 1 → simple Attend sheet; 2+ → scrollable list of cards, each with an inline Attend. Never stacked modals. |
| 4 | Re-appearance | Suppress on **check-in OR dismiss**, persisted locally (no server `/me`). |
| 5 | Un-RSVP / withdraw | **Not supported (MVP).** No member cancel endpoint exists; once attended it is final (consistent with attendance). Explicit non-goal. |

---

## 3. Consumed API (member-facing)

Base path: `/events/rsvps` (the shared Ktor client prepends `BACKEND_URL` + `/api/v1`).

| Method | Path | Notes |
|--------|------|-------|
| GET | `/events/rsvps/active` | RSVPs with an open window now. Projection: `publicId, title, windowStart, windowEnd, announcementId?`. |
| POST | `/events/rsvps/{publicId}/check-in` | No body. 201 confirm, 409 already-RSVPed, 400 outside window. |

`windowStart`/`windowEnd` are ISO-8601 offset datetimes (e.g. `2026-07-12T09:00:00+09:00`).
`announcementId` is nullable.

Admin/leader endpoints (`POST /`, `PATCH`, `DELETE`, `GET /`, `GET /{id}/attendees`)
are **not** called by the mobile app.

---

## 4. Package Layout

New top-level feature package — no changes to `features/attendance/`:

```
features/events/
  domain/
    model/EventRsvp.kt          // publicId, title, windowStart, windowEnd, announcementId?
    model/EventRsvpCheckIn.kt   // eventPublicId, eventTitle, checkedInAt
    repository/EventRsvpRepository.kt
  data/
    model/EventRsvpResponse.kt          // GET /active item DTO
    model/EventRsvpCheckInResponse.kt   // POST /check-in data DTO
    repository/EventRsvpRepositoryImpl.kt
  presentation/
    EventRsvpViewModel.kt
    EventRsvpUiState.kt
    EventRsvpHost.kt                     // mount point + lifecycle trigger
    components/EventRsvpSheet.kt         // the adaptive ModalBottomSheet
```

Shared infra:
```
core/domain/repository/EventRsvpPreferences.kt   // seen/checked-in per publicId
core/data/repository/EventRsvpPreferencesImpl.kt // multiplatform-settings, mirrors AttendancePreferencesImpl
```

DI: bind `EventRsvpRepository`, `EventRsvpPreferences`, and `EventRsvpViewModel` in
`di/AppModule.kt`.

---

## 5. State & ViewModel

```kotlin
data class EventRsvpUiState(
    val events: List<EventRsvp> = emptyList(),   // active, not yet acted on
    val visible: Boolean = false,                // sheet shown?
    val checkingInId: String? = null,            // publicId currently in-flight
    val checkedInIds: Set<String> = emptySet(),  // show "참석 완료 ✓" before removal
    val rowError: Map<String, String> = emptyMap(),
)
```

`EventRsvpViewModel`:

- **`refresh()`** — `GET /active`; filter out any `publicId` already in
  `EventRsvpPreferences` (checked-in **or** dismissed). If the surviving list is
  non-empty → `visible = true`. On network failure: log and no-op (never blocks Home).
- **`checkIn(publicId)`** — guard against double-tap via `checkingInId`; `POST check-in`:
  - **201** → `prefs.markCheckedIn(publicId)`; flip row to "참석 완료 ✓"; remove after a
    short spring delay; close sheet when the list empties.
  - **409** → same as success (server already has them): persist + ✓.
  - **400** → `rowError[publicId] = "지금은 참석 가능하지 않습니다. 나중에 다시 시도해주세요."`,
    then call `refresh()` (window likely closed → event drops off the active list).
  - **other/5xx** → `rowError[publicId] = "참석 처리에 실패했습니다"`; row stays for retry.
- **`dismiss(publicId)`** — `prefs.markDismissed(publicId)`; drop from list.
  Dismissing the whole sheet ("나중에") marks all currently-shown events dismissed.

**Local dedup rationale:** backend has no "did I check in" endpoint (same as
attendance). Local persistence is the only way to avoid re-nagging across launches.
Keyed by `publicId` only — events are one-off, so no date composite is needed.

---

## 6. Triggers

A single **`EventRsvpHost`** composable is mounted once in `App.kt`, layered above the
`NavHost` (sibling of `FloatingPillNav`), rendered only on top-level destinations. It
owns the `EventRsvpViewModel` and calls `refresh()`:

1. on first composition after reaching Home, and
2. on app return-to-foreground, via `LifecycleEventObserver(Lifecycle.Event.ON_START)`.

This keeps the trigger out of individual screens and decoupled from `HomeViewModel`.

**Second trigger — announcement CTA:** in `AnnouncementDetailScreen`, when
`announcement.category == "EVENT"`, show a primary **행사 참석하기** CTA in the article
body. Tapping opens the same `EventRsvpSheet`, scoped to the RSVP whose
`announcementId == announcement.id`. If no active RSVP matches (window closed, or the
backend `announcementId` field is not yet live) the CTA is **hidden** (not disabled) so
the feature ships harmlessly before the backend field lands.

---

## 7. Sheet UI (`EventRsvpSheet`) — DESIGN.md "Warm Premium"

- `ModalBottomSheet`: `shape_large` (20dp) top corners, `surface_container_lowest`
  background, drag handle, ambient shadow (floating element). Caps at ~60% height.
- **1 event:** title (`title_large`); window line e.g. "오늘 09:00 – 12:00"
  (`body_medium`, `on_surface_variant`); full-width **참석하기** primary gradient pill
  (`shape_full`, press scale → 0.97 spring); low-emphasis **나중에** text button.
- **2+ events:** header "참석할 행사"; scrollable column of Standard Cards
  (`shape_medium`, ghost border, `space_sm` gaps) — each = title + window + trailing
  **참석** pill. One **나중에** at the bottom dismisses all.
- **Per-row states:** in-flight → `CircularProgressIndicator` inside the pill; success →
  pill/row swaps to check + "참석 완료" (spring), then animates out (`AnimatedVisibility`
  + list-stagger spring); 400/error → `body_small` error line under the row.
- Sections separated by **surface shifts, never 1px divider lines**. Every animation uses
  a `spring()` spec (no linear / ease-in-out).
- All user-facing strings via `LocalStrings` (KO/EN/DE). Window formatting lives in the VM.

---

## 8. Backend dependency (precondition)

`GET /events/rsvps/active` must include **`announcementId`** in each item's projection.
The dn-server spec's `active` response currently returns only
`publicId, title, windowStart, windowEnd`. Without `announcementId` the mobile app
still works for the auto-on-Home prompt; only the announcement-detail CTA stays hidden.
Track this as the single coordinating change with dn-server.

---

## 9. Error handling & edge cases

- `GET /active` failure → silent (log only); no sheet. Prompt is non-critical.
- Check-in 201/409 → both resolve to "참석 완료 ✓" + local persist.
- Check-in 400 → row error "지금은 참석 가능하지 않습니다. 나중에 다시 시도해주세요." + `refresh()`.
- Check-in other/5xx → row error "참석 처리에 실패했습니다"; row stays for retry.
- Window expires while sheet open → handled by the 400 path on tap + foreground
  `refresh()`. **No client-side countdown timer** (avoids clock-skew bugs).
- Empty active list → sheet never shows; no empty state.

---

## 10. Testing

`commonTest` (JVM/Android; iOS validated via CI per `tasks/lessons.md`).

**`EventRsvpViewModelTest`** with `FakeEventRsvpRepository` + `FakeEventRsvpPreferences`:
- `refresh` filters out already-checked-in and already-dismissed events
- `refresh` hides sheet when filtered list is empty
- `checkIn` 201 → persists + marks row checked-in
- `checkIn` 409 → treated as success (persists, ✓)
- `checkIn` 400 → row error + does NOT mark checked-in + triggers refresh
- double-tap guard: second `checkIn` while in-flight is a no-op
- `dismiss` persists + removes; dismiss-all clears the sheet

**`EventRsvpRepositoryImplTest`** with Ktor `MockEngine`:
- `GET active` parses `data[]` including nullable `announcementId`
- `POST check-in` uses `expectSuccess = true` so 4xx surface as `ClientRequestException`

**Verification gate:** `./gradlew :composeApp:testDevDebugUnitTest` green +
`./gradlew lint` (no new errors; no `TODO` in `composeApp/src`) before Done.

---

## 11. Non-goals

- Admin/group-leader RSVP management on mobile.
- Member un-RSVP / withdraw (no backend endpoint; MVP-final once attended).
- Client-side window countdown timer.
- Attendee-list viewing on mobile.
