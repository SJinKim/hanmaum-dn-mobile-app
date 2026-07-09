# Spec: Profile Hub + Settings Screen + Personal-Info (view = edit) Redesign

**Date:** 2026-07-09 · **Status:** Confirmed — user approved defaults 2026-07-09 (birthdate editable, photo URL field stays). Plan: `docs/superpowers/plans/2026-07-09-profile-hub-settings-redesign.md`
**Reference research:** `.lazyweb/quick-references/profile-settings-2026-07-09/report.md` (user approved direction 2026-07-09)

## 1. Why

User decision after reviewing real-app references: personal data should not sit exposed
on the profile tab. Industry pattern (Kayak/Spotify/Hulu hub + Adidas/Telegram combined
view-edit form) fits DN. The current view-mode info-card list and the separate edit
mode are replaced by: **hub tab → Personal-Info screen (prefilled editable form) +
Settings screen (new)**. The edit surface gets the "professional" treatment to match
the app's other screens (Warm Premium, DESIGN.md).

## 2. Screens

### 2.1 Profile tab (hub) — rework of `ProfileScreen` view mode
- Hero block on `surface`: avatar, full name (`headline`), **church division** under the
  name (`body_medium`, from the member's group division, e.g. "2교구"), and a chip with
  the **church group name** (e.g. "믿음 목장"). No ACTIVE/status chip, no address/phone/
  email anywhere on this tab.
- Menu group on `surface_container_low` (No-Line Rule): rows with chevrons —
  **개인 정보 (Personal info)** → PersonalInfoRoute, **설정 (Settings)** → SettingsRoute.
- 로그아웃 (Log out) stays at the bottom of the hub (Hulu pattern), destructive styling
  as today.
- Division line and group chip are hidden when null (PENDING members may have no group).

### 2.2 Settings screen — NEW (`features/profile/presentation/SettingsScreen.kt`)
- Receives everything that lives in the profile tab's settings block today:
  언어 (locale), 테마 (theme), Face ID toggle, 로그인 유지 (keep signed in).
- Detail-screen chrome: chevron-left back (44dp) + system swipe-back; sections grouped
  by surface shift; UPPERCASE `label` group titles.
- No new business logic — the existing callbacks (`onLocaleChange`, `onThemeChange`,
  `onBiometricToggle`, `onKeepSignedInToggle`) move with the UI.

### 2.3 Personal-Info screen — NEW, replaces both view info-cards and edit mode
- One screen, **view = edit** (Adidas/Telegram pattern):
  - **Locked rows** (visible, muted + lock icon, not editable): name, email,
    division, group name, church role. These are admin-managed; shown so members can
    verify what the church has on file (GOAT/WeWork pattern).
  - **Editable prefilled fields**: profile image URL (until the S3 gallery upload
    feature lands), phone, **birthdate via `DatePickerDialog`** (same Material3
    pattern as `RegisterScreen.kt:575-598`), street, house number, zip code, city.
  - Single **저장 (Save)** button, enabled only when the form is dirty
    (current values ≠ loaded profile). No Cancel button needed — back discards.
  - Save success → snackbar/inline confirmation, fields re-seed from response.
- Existing `ProfileViewModel` edit state carries over; add `editBirthDate`.

## 3. Backend contract changes required (server repo — needs its own PR)

Verified against `hanmaum-dn-server` @ main:

| Data | Server today | Change needed |
|---|---|---|
| Division | `ChurchGroup.division: String?` exists (`features/groups/domain/ChurchGroup.kt:13`) but `/members/me` `MemberResponse` (MemberDtos.kt:106-120) does **not** expose it | Add `val division: String? = null` to server `MemberResponse` + mapper (`member.group?.division`) |
| Birthdate (read) | `Member.birthDate: LocalDate?` exists; not in `MemberResponse` | Add `val birthDate: LocalDate? = null` to `MemberResponse` + mapper |
| Birthdate (write) | Not in `UpdateMyProfileRequest` (MemberDtos.kt:206-215) | Add `val birthDate: LocalDate? = null`; apply via `request.birthDate?.let { member.birthDate = it }` (null=keep, consistent with siblings) |

**Mapping rules (the zip_code lesson, tasks/lessons.md):** all wire names camelCase
(`division`, `birthDate`); birthdate serialized `"YYYY-MM-DD"` (same as
`RegisterRequest.birthDate: String? // Format: YYYY-MM-DD` — mobile models it as
String, not a date type). Mobile MockEngine tests must assert exact body keys for the
extended PATCH. Mobile parsing must tolerate the fields being absent (defaults null)
so the app works against an un-upgraded backend — division/birthdate rows simply hide.

## 4. Mobile changes

- `MemberResponse` (mobile) += `division: String? = null`, `birthDate: String? = null`.
- `UpdateMyProfileRequest` += `birthDate: String? = null`; repository + ViewModel pass-through.
- `Routes.kt` += `PersonalInfoRoute`, `SettingsRoute` (`@Serializable`); `composable<>`
  registrations in `App.kt`. Profile stays the tab root.
- `ProfileScreen` slims to the hub; new `PersonalInfoScreen.kt`, `SettingsScreen.kt`
  in `features/profile/presentation/`.
- i18n: new strings (personal info title, settings title, locked-field hint, save
  confirmation, birthdate label exists?) in `AppStrings` + KO + EN + DE.
- Tests: ViewModel tests extended (birthdate edit, dirty-flag save enablement),
  repository MockEngine body-key test extended with `birthDate`.

## 5. Defaults chosen — confirm or override

| # | Question | Default chosen |
|---|---|---|
| 1 | Is birthdate member-editable (vs. locked, admin-only)? | **Editable** (user asked for the datepicker) — requires the server PATCH change above |
| 2 | Where does Log out live? | **Hub bottom** (Hulu pattern), not inside Settings |
| 3 | Church role / member status display | Role = locked row on Personal-Info only; status not shown anywhere (members are always ACTIVE when they see this screen) |
| 4 | Profile photo editing until S3 upload exists | Keep the URL text field on Personal-Info; swap for gallery picker when the S3 feature lands |
| 5 | Division/group missing (null) | Hide the line/chip on the hub; on Personal-Info show locked row with "—" |
| 6 | Save UX | Dirty-only enabled Save, back = discard (no Cancel button, no confirm dialog) |

## 6. Out of scope

- Gallery photo picker + S3 upload (separate feature, backend missing; overlaps album
  S3 migration spec 2026-07-02).
- Editing name/email/group/division/role from the app (admin-managed).
- Notifications settings row (no notification preferences exist yet).

## 7. Order of work

1. Server PR: `division` + `birthDate` in `MemberResponse`, `birthDate` in PATCH.
2. Mobile PR (can start behind null-tolerant parsing): hub rework + Settings screen +
   Personal-Info screen + DTO extensions + tests.
Builds on `fix/profile-edit-house-number` (PR #83) — merge that first.
