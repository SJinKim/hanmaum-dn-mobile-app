# Ministry Page — Design Spec

**Date:** 2026-06-10
**Status:** Approved (pending spec review)
**Feature:** `features/ministry`

## 1. Goal & Scope

Re-introduce the **Ministry (사역)** feature as an **informational** experience: a directory of
the church's ministries and a detail page describing what each one does. The feature was
previously built (old "Luminous Sanctuary" design with a registration/apply flow) and then
removed from the app's reachable surface; the dead code still lives under `features/ministry`.

**In scope**
- A ministry **directory** (list) reached as a bottom-nav tab.
- A ministry **detail** page (About / Requirements / Schedule / Contact).
- Full Warm Premium v2 redesign per `designs/dn_app/DESIGN.md`.
- Re-alignment of the data layer to the real backend contract.

**Explicitly out of scope (removed)**
- Registration / apply flow (PENDING / APPROVED states, registration bottom sheet).
- `MyRegistration`, `RegistrationStatus`, `CreateRegistrationRequest`, `RegistrationResponse`,
  and the registration repository methods/endpoints.
- The `GET /ministries/{publicId}/members` endpoint exists on the backend but is **not used**.

## 2. Entry Point — Bottom Navigation Swap

`core/navigation/TopLevelDestination.kt` currently exposes
`all = [Home, News(소식 → AnnouncementListRoute), Calendar, Album, Profile]`.

- **Remove** `News` from `TopLevelDestination.all`.
- **Add** `Ministry` in the same slot:
  - `routeInstance = MinistryListRoute`, `routeClass = MinistryListRoute::class`
  - `label = "사역"`
  - `icon = Icons.Default.VolunteerActivism`
- Resulting order: `[Home, Ministry, Calendar, Album, Profile]`.

Consequences:
- The floating pill nav shows on `MinistryListRoute` (it becomes a top-level destination) and is
  hidden on `MinistryDetailRoute` (a pushed sub-screen) — driven by the existing `showBottomBar`
  logic in `App.kt` (`TopLevelDestination.all.any { hasRoute(...) }`).
- **News content is not lost.** `AnnouncementListScreen` is still reachable via
  **Home → 전체보기** (`HomeScreen.onViewAllClick → AnnouncementListRoute`). It already accepts an
  `onBackClick` and works as a pushed screen. No change to `AnnouncementListRoute`'s `composable`.
- The home **빠른 메뉴** (교회 지도 / floor plan) section is left untouched.

## 3. Data Model & API Contract

All responses are wrapped as `{ success, message, data }` and consumed through the existing
`core/domain/model/ApiResponse<T>` (`body.data`), as the current repository already does.

### Endpoints
- `GET /ministries?active=true` → `ApiResponse<List<MinistrySummaryResponse>>`
- `GET /ministries/{publicId}` → `ApiResponse<MinistryDetailResponse>`

### DTOs (`data/model`, `@Serializable`, JSON-verbatim field names)
```kotlin
@Serializable
data class MinistrySummaryResponse(
    val publicId: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String? = null,
    val contacts: List<ContactResponse> = emptyList(),
    val active: Boolean,
)

@Serializable
data class MinistryDetailResponse(
    val publicId: String,
    val title: String,
    val subtitle: String,
    val about: String,                       // non-nullable per backend
    val requirements: List<String> = emptyList(),
    val schedules: List<ScheduleResponse> = emptyList(),
    val contacts: List<ContactResponse> = emptyList(),
    val imageUrl: String? = null,
    val active: Boolean,
)

@Serializable
data class ScheduleResponse(
    val description: String,
    val startTime: String,                   // "07:00"
    val endTime: String,                     // "09:00"
)

@Serializable
data class ContactResponse(
    val role: String,                        // "팀장", "간사", "부팀장", "팀원" …
    val name: String,
)
```
Field names match the JSON exactly, so no `@SerialName` is required. (Domain names such as
`imageUrl`/`active` could be remapped via `@SerialName`, but keeping DTO == JSON is simpler.)

### Domain models (`domain/model/Ministry.kt`)
```kotlin
data class Ministry(                          // list summary
    val publicId: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val contacts: List<Contact>,
    val isActive: Boolean,
)

data class MinistryDetail(
    val publicId: String,
    val title: String,
    val subtitle: String,
    val about: String,
    val requirements: List<String>,
    val schedules: List<Schedule>,
    val contacts: List<Contact>,
    val imageUrl: String?,
    val isActive: Boolean,
)

data class Schedule(val description: String, val startTime: String, val endTime: String)
data class Contact(val role: String, val name: String)
```
Mapper (`MinistryRepositoryImpl`) translates `active → isActive`; everything else is a direct copy.
The old `MyRegistration` / `RegistrationStatus` types are deleted.

> **Note on stale DTOs:** the current mobile DTOs (`name` / `shortDescription` / `longDescription`
> / `leader`) predate the feature's removal and no longer match the backend. They are replaced
> wholesale by the DTOs above.

### Repository (`domain/repository/MinistryRepository.kt`)
```kotlin
interface MinistryRepository {
    suspend fun getMinistries(activeOnly: Boolean = true): Result<List<Ministry>>
    suspend fun getMinistryDetail(publicId: String): Result<MinistryDetail>
}
```
`getMyRegistration` and `register` are removed. `MinistryRepositoryImpl` drops the
`kotlinx-datetime` / `Clock` usage that existed only for registration.

## 4. Screens (Warm Premium — `DESIGN.md` tokens)

### 4.1 Directory — `MinistryListScreen` (top-level tab)
- **No back chevron** (it's a tab). Page header: eyebrow label, `사역` title (`display`/`headline`),
  one-line subtitle in `on_surface_variant`.
- **Cards (approved "Option A — compact icon row")**, one per ministry:
  - Leading gradient icon tile (`primary → primary_dark`, `shape_medium`) with a single consistent
    ministry glyph (e.g. `Groups`/`VolunteerActivism`) — no per-ministry glyph data exists.
  - `title` (`title_large`/`title_medium`), one-line `subtitle` (`body`, 2-line max, ellipsis),
    trailing chevron (`muted`).
  - `surface_container_lowest` card on `surface`, `shape_medium`, tonal depth, **no divider lines**.
  - `contacts` are present in the model but **not shown** on the card (Option A).
- **Motion:** list-stagger entry `spring(0.85, 260)`, 40ms/item, cap 5; card press scale `0.97`
  `spring(0.6, 400)`.
- **States:** Loading (centered indicator) · Error (`ErrorView` + retry) · Empty
  ("등록된 사역이 없습니다") · Success.
- Last item `paddingBottom = 80dp` (`space_bottom_nav`) to clear the floating pill.

### 4.2 Detail — `MinistryDetailScreen` (pushed sub-screen)
- **Hero:** uses `imageUrl` when present; otherwise the `primary → primary_dark` diagonal gradient
  with the top-right circular glare overlay. Hosts the **back chevron** (`<`, `muted`→`on_surface`
  on press, ≥44dp target). iOS swipe-back + Android predictive back remain active.
- **Header block:** eyebrow label, `title` (with any parenthetical kept inline), `subtitle` tagline.
- **Four sections**, each rendered in a `surface_container_lowest` card, **auto-hidden when its
  data is empty/blank**:
  1. **소개 (About)** — `about` split into paragraphs on blank lines.
  2. **지원 자격 (Requirements)** — one plain text row per string; rows separated by a hairline
     (`outline_variant` @ 15%); **no leading markers/checks**.
  3. **일정 (Schedule)** — one row per schedule: `description` on the left, a
     `startTime–endTime` pill (`primary`-tinted, `shape_full`) on the right.
  4. **문의 (Contact)** — one row per contact: `role` label on the left
     (`label` style, `muted`), `name` on the right (`title_medium`). **No avatar circles.**
- Section headers: small icon + Korean label, sourced from `AppStrings` (i18n).
- **Motion:** screen push `spring(0.80, 250)`.

## 5. Plumbing

- **ViewModels** (`presentation/*`, `ViewModel` + `StateFlow`):
  - `MinistryListViewModel` → `loadMinistries()`; `MinistryListUiState` = Loading / Error(message) /
    Success(`List<Ministry>`).
  - `MinistryDetailViewModel(publicId)` → `load()`; `MinistryDetailUiState` = Loading /
    Error(message) / Success(`MinistryDetail`). All registration/sheet state removed.
- **DI** (`di/AppModule.kt`): keep the `MinistryRepository` and both ViewModel bindings; remove any
  registration-specific wiring. `MinistryRepositoryImpl(client)` constructor unchanged in shape.
- **Navigation** (`App.kt`): `composable<MinistryListRoute>` no longer passes `onBackClick`
  (it's a tab; the screen signature drops it). `composable<MinistryDetailRoute>` unchanged.
  `MinistryListRoute` / `MinistryDetailRoute` already exist in `Routes.kt`.
- **i18n** (`core/i18n/AppStrings.kt`): add section labels (소개 / 지원 자격 / 일정 / 문의),
  the empty-state string, and remove now-unused registration strings if they are not referenced
  elsewhere.

## 6. Files Touched

Rewritten:
- `domain/model/Ministry.kt`, `domain/repository/MinistryRepository.kt`
- `data/model/MinistrySummaryResponse.kt`, `data/model/MinistryDetailResponse.kt`
- `data/repository/MinistryRepositoryImpl.kt`
- `presentation/list/{MinistryListScreen,MinistryListViewModel,MinistryListUiState}.kt`
- `presentation/detail/{MinistryDetailScreen,MinistryDetailViewModel,MinistryDetailUiState}.kt`

Deleted:
- `data/model/CreateRegistrationRequest.kt`, `data/model/RegistrationResponse.kt`

Edited:
- `core/navigation/TopLevelDestination.kt` (News → Ministry)
- `App.kt` (drop `onBackClick` on the ministry list composable)
- `di/AppModule.kt`, `core/i18n/AppStrings.kt`
- New `data/model/{ScheduleResponse,ContactResponse}.kt` (or nested in the detail/summary files)

## 7. Verification

- `./gradlew :composeApp:testDevDebugUnitTest` (flavored task name; bare names are ambiguous).
- `./gradlew lint` — note the **3 pre-existing** geofence/notification lint errors are not
  regressions from this change; confirm no new errors.
- Confirm no `TODO` string under `composeApp/src` (CI greps for it).
- iOS link/run validated via the PR's `ios-check` (cannot be validated locally with CLT only).
- Manual: tab swap shows 사역 in the bottom nav; list → detail navigation; empty-section hiding;
  empty-list state.

## 8. Open Defaults (easily changed)

- Tab icon = `VolunteerActivism` (alt: `Groups`).
- Section labels in Korean (소개 / 지원 자격 / 일정 / 문의).
- List card does not surface `팀장` (contacts hidden on the directory per Option A).
