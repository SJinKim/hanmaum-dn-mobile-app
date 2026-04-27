# Bottom Navigation Bar — Design Spec

**Date:** 2026-04-27  
**Status:** Approved

## Goal

Show a persistent bottom navigation bar on every main tab screen. Hide it on all detail screens, pre-auth screens, and system screens (Splash, Login, Register, Pending).

Tapping the active tab returns the user to the root of that tab. The implementation must be reusable, maintainable, and use descriptive, framework-agnostic naming (no `Church-` prefix).

---

## Screens

### Bottom bar visible
| Screen | Tab |
|---|---|
| `HomeScreen` | HOME |
| `CommunityStubScreen` | COMMUNITY |
| `MinistryListScreen` | MINISTRIES |
| `AnnouncementListScreen` | NEWS |
| `ProfileScreen` | PROFILE |

### Bottom bar hidden
- `AnnouncementDetailScreen`
- `MinistryDetailScreen`
- `SplashScreen`, `LoginScreen`, `RegisterScreen`, `PendingScreen`

---

## Architecture

### Single Scaffold approach (Approach A)

One `Scaffold` wraps the entire `NavHost` in `App.kt`. The bottom bar visibility is driven by checking if the current `NavDestination` matches any entry in `TopLevelDestination.all`. No nested NavHosts, no secondary NavControllers.

---

## Components

### 1. `TopLevelDestination` (new)
**File:** `core/navigation/TopLevelDestination.kt`

A sealed class where each `data object` represents one bottom tab. Properties:
- `route: KClass<T>` — the navigation route class
- `icon: ImageVector` — Material icon
- `label: String` — Korean display label

A `companion object` exposes `all: List<TopLevelDestination<*>>` as the single source of truth for all tabs. Adding a new tab = one new `data object` entry. Nothing else changes.

Entries: `Home`, `Community`, `Ministries`, `News`, `Profile`

### 2. `BottomNavBar` (rename + refactor of `ChurchBottomBar`)
**File:** `core/presentation/components/BottomNavBar.kt`  
**Old file deleted:** `ChurchBottomBar.kt`

Signature:
```kotlin
@Composable
fun BottomNavBar(
    currentDestination: NavDestination?,
    onDestinationSelected: (TopLevelDestination<*>) -> Unit,
)
```

Iterates `TopLevelDestination.all` to render `NavigationBarItem`s. Selected state determined by `currentDestination?.hasRoute(dest.route, null) == true`. No hardcoded tab list inside the component.

### 3. `App.kt` (updated)

- Wrap `NavHost` in a `Scaffold`
- Observe current destination via `navController.currentBackStackEntryAsState()`
- `showBottomBar` = any `TopLevelDestination.all` entry matches current destination
- Pass `innerPadding` from `Scaffold` to `NavHost` via `Modifier.padding(innerPadding)`

Tab selection logic:
```kotlin
navController.navigate(dest.route) {
    popUpTo(navController.graph.startDestinationId) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

- `launchSingleTop = true` — prevents duplicate stack entries; re-navigating to the current tab pops to its root
- `saveState = true` / `restoreState = true` — preserves scroll/state when switching tabs

---

## Naming Convention

All new and modified components use descriptive, framework-agnostic names. No `Church-` prefix.

| Old name | New name |
|---|---|
| `ChurchBottomBar` | `BottomNavBar` |
| `BottomTab` (enum) | removed — replaced by `TopLevelDestination` |

`ChurchTopBar` is out of scope for this change. Rename separately when that component is next touched.

---

## What does NOT change

- Existing `HomeScreen` callbacks (`onMinistryClick`, `onCommunityClick`, etc.) remain unchanged. They handle deep navigation; the bottom bar handles top-level tab switching independently.
- No changes to routes, ViewModels, repositories, or any other layer.
