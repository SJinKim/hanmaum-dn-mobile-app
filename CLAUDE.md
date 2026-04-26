# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

hanmaum-dn-mobile-app is a **Kotlin Multiplatform (KMP)** mobile app targeting Android and iOS, built with **Compose Multiplatform** for shared UI. It is a church app (한마음 DN) with authentication via Keycloak, member management, and announcements.

## Build Commands

```bash
# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Run all tests
./gradlew :composeApp:allTests

# Run a single test class
./gradlew :composeApp:testDebugUnitTest --tests "com.hanmaum.dn.mobile.YourTestClass"

# Clean build
./gradlew clean
```

For iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run from there.

## Architecture

### Layer Structure (per feature)
Each feature under `features/<name>/` follows clean architecture:
- `domain/model/` — plain data classes and interfaces
- `domain/repository/` — repository interfaces
- `data/repository/` — Ktor-based repository implementations
- `presentation/` — ViewModels (`ViewModel` + `StateFlow`) and Compose screens

Shared infrastructure lives in `core/`:
- `core/network/` — Ktor `HttpClient` creation with bearer auth plugin
- `core/domain/repository/TokenStorage` — interface for token persistence
- `core/data/repository/TokenStorageImpl` — **in-memory only** (tokens lost on restart — TODO: replace with persistent storage)
- `core/navigation/Routes.kt` — type-safe `@Serializable` navigation route objects
- `core/domain/model/NavRoute.kt` — platform-agnostic `enum` used by ViewModels to signal navigation intent

### Dependency Injection
Koin is used throughout. All bindings are in `di/AppModule.kt`. On Android, Koin is started in `DnChurchApp` (Application class). On iOS, it is started via `KoinInit.kt`. `App()` composable wraps everything in `KoinContext {}`.

### Navigation Flow
`App.kt` hosts a single `NavHost`. The start destination is always `SplashRoute`. `SplashViewModel` checks the stored token and member status, then emits a `NavRoute` to navigate to `HomeRoute`, `LoginRoute`, or `PendingRoute`.

Member statuses: `ACTIVE` → Home, `PENDING` → Pending approval screen, `REJECTED`/`DELETED` → clears tokens and goes to Login.

### Networking
`createHttpClient(tokenStorage)` in `core/network/NetworkClient.kt` configures the shared Ktor client:
- **Base URL injection**: If the request URL has no host, it prepends `BuildKonfig.BACKEND_URL` and wraps the path as `/api/v1/<path>`. Login (Keycloak) requests use absolute URLs and bypass this.
- **Bearer auth**: Auto-attaches access token; skipped for paths containing `"register"` or `"openid-connect"`.
- **Auth backend**: Keycloak at `BuildKonfig.KEYCLOAK_URL`, realm `hanmaum`, client `hanmaum-mobile`, `password` grant type.

### BuildKonfig (environment URLs)
URLs are injected at compile time via the `buildkonfig` plugin:

| Target  | `BACKEND_URL`           | `KEYCLOAK_URL`           |
|---------|-------------------------|--------------------------|
| Android | `http://10.0.2.2:8080`  | `http://10.0.2.2:8091`   |
| iOS     | `http://localhost:8080`  | `http://localhost:8091`  |

To change URLs, edit the `buildkonfig {}` block in `composeApp/build.gradle.kts`.

### Key Tech Stack
- **UI**: Compose Multiplatform 1.10.0, Material3
- **Navigation**: `org.jetbrains.androidx.navigation:navigation-compose` (type-safe routes)
- **Networking**: Ktor 3.3.3 (OkHttp on Android, Darwin on iOS)
- **DI**: Koin 4.0.0
- **Serialization**: `kotlinx-serialization-json`
- **ViewModel**: `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`
