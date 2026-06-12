# Calendar Event List Screen — Design Spec

**Date:** 2026-05-10  
**Status:** Approved

---

## 1. Overview

Add a **List tab** to the existing `CalendarScreen` that shows all events for the current year, grouped by month. The screen is a second *view* of the same calendar data — not a separate navigation destination.

---

## 2. Navigation & Entry Point

- The existing `CalendarRoute` and `CalendarScreen` are the only entry points. No new route is added.
- A **pill segmented control** is added directly below the "캘린더" screen title, replacing the month navigation row when the list tab is active.
- Two segments: **📅 캘린더** (left) | **☰ 목록** (right).
- Active segment: filled `primary` (`#ae2f34`) background, white text.
- Inactive segment: `#6b6b6b` text on `#ebebeb` pill background.
- Tab switch animation: `animateFloatAsState` with `spring()` spec (no linear/ease transitions).

---

## 3. List View Layout

When "목록" is active, the month navigation arrows are hidden and a `LazyColumn` fills the screen body.

### 3.1 Month Header

Each of the 12 months of the current year has a header:

| Property | Value |
|---|---|
| Month name | Korean (e.g. `1월` … `12월`) |
| Font | Plus Jakarta Sans, `22sp`, weight `800`, letter-spacing `-0.02em` |
| Color (default) | `#1c1b1f` |
| Color (current month) | `primary` `#ae2f34` |
| Event count | Right-aligned, `11sp`, weight `600`, uppercase, `#9e9e9e` (e.g. `3개`) |
| Top padding | `20dp`, bottom padding `10dp` |
| Background | `#f3f3f3` (`surface_container_low`) — inherits from screen background |

### 3.2 Event Card

Each event within a month renders as a white card:

| Property | Value |
|---|---|
| Background | `#ffffff` (`surface_container_lowest`) |
| Corner radius | `16dp` |
| Shadow | `0 2px 8px rgba(45,52,54,0.04)` |
| Margin bottom | `8dp` |
| Internal padding | `14dp × 16dp` |
| Column horizontal padding | `16dp` on each side (applied to `LazyColumn` content padding) |
| Left column | Day number in `primary` (`800` weight, `20sp`) + Korean day-of-week abbreviation (`9sp`, `#c0c0c0`, uppercase) |
| Right area | Title (`14sp`, weight `700`, `#1c1b1f`) + time and location on a second line (`11sp`, `#9e9e9e`) |

- Tapping a card sets `selectedEvent` in state, opening the **existing bottom sheet detail** (no new UI).

### 3.3 Empty Month Card

Months with no events still appear with the same card shape, showing italic `이벤트 없음` in `#d0d0d0`.

### 3.4 No Dividers

No `Divider` composables anywhere. Separation is achieved by card spacing and the `surface_container_low` background.

---

## 4. Scroll Behaviour

When the List tab is first opened, `LazyColumn` auto-scrolls so the **current month** header is at the top of the viewport (`LazyListState.scrollToItem` or `animateScrollToItem` targeting the current month's index). Opening in January shows the top naturally; opening in December scrolls to December.

---

## 5. Data Layer

### 5.1 New repository method

```kotlin
// CalendarRepository.kt
suspend fun getYearEvents(year: Int): Result<List<CalendarEvent>>
```

```kotlin
// CalendarRepositoryImpl.kt
override suspend fun getYearEvents(year: Int): Result<List<CalendarEvent>> {
    // timeMin = {year}-01-01T00:00:00Z
    // timeMax = {year+1}-01-01T00:00:00Z
    // Same Google Calendar API call, singleEvents=true, orderBy=startTime, maxResults=500
}
```

Single API call for the entire year — no per-month loop.

---

## 6. ViewModel Changes

### 6.1 New state fields in `CalendarUiState`

```kotlin
enum class ViewMode { CALENDAR, LIST }

data class CalendarUiState(
    // ... existing fields unchanged ...
    val viewMode: ViewMode = ViewMode.CALENDAR,
    val yearEvents: List<CalendarEvent> = emptyList(),
    val yearEventsLoaded: Boolean = false,
    val isYearLoading: Boolean = false,
)
```

### 6.2 New method in `CalendarViewModel`

```kotlin
fun switchView(mode: ViewMode) {
    _uiState.update { it.copy(viewMode = mode) }
    if (mode == ViewMode.LIST && !uiState.value.yearEventsLoaded) {
        loadYearEvents()
    }
}
```

- `loadYearEvents()` is called **lazily** — only on first switch to LIST. On success or empty result, `yearEventsLoaded` is set to `true` so subsequent tab switches do not re-fetch.
- `isYearLoading` drives a loading indicator in the list view while the year data fetches.
- `isYearLoading` drives a loading indicator in the list view while the year data fetches.

---

## 7. CalendarScreen Changes

- Add `viewMode` observation from `uiState`.
- Render the pill toggle below the title, always visible.
- When `viewMode == CALENDAR`: show existing month navigation + calendar grid + day-filtered events list (unchanged).
- When `viewMode == LIST`: hide month navigation arrows; show the `LazyColumn` with month sections built from `yearEvents`.

---

## 8. Out of Scope

- Year navigation (always current year — determined at ViewModel init from system clock).
- Search or filter within the list.
- Pull-to-refresh for the year view (manual re-fetch not needed for this version).
- Any change to the event detail bottom sheet.
