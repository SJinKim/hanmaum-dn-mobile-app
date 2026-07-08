---
name: scaffolding-feature-slices
description: Use when adding a new feature, screen, or API-backed capability to the app, before creating any files — or when a screen compiles but crashes at runtime with a Koin "no definition found" or navigation error, which usually means a wiring step below was skipped.
---

# Scaffolding Feature Slices

## Overview

Every feature here is the same machine with different data. The expensive
failure mode is not writing bad code — it's inventing a second pattern, or
compiling clean and then crashing at runtime because one of the **nine wiring
points** was missed (Koin binding, route registration, translations…).

**Reference implementations, newest first: `features/events/` (sheet-based,
typed check-in results), `features/attendance/` (screen-based, local
preferences). When in doubt, do what they do.**

## File tree (copy this shape)

```
features/<name>/
  domain/model/       Foo.kt, FooResult.kt          # plain classes, sealed results, NO @Serializable
  domain/repository/  FooRepository.kt              # interface; Result<T> or sealed result — never throws
  data/model/         FooResponse.kt                # @Serializable DTOs, wire-shape only
  data/repository/    FooRepositoryImpl.kt          # shared HttpClient; unwraps ApiResponse<T>
  presentation/       FooViewModel.kt, FooUiState.kt, FooScreen.kt, components/
commonTest/.../features/<name>/
  FakeFooRepository.kt, FooViewModelTest.kt
  data/repository/FooRepositoryImplTest.kt          # Ktor MockEngine
```

## The nine wiring points (each one missed = runtime failure or review reject)

1. Domain models + repository interface (typed sealed result for multi-outcome
   calls — see `CheckInResult`).
2. DTOs — wire casing via `@SerialName`, nullable-with-default for fields the
   backend may omit. Unwrap the `ApiResponse<T>(success, message, data)`
   envelope inside the repo impl; the client has `expectSuccess = false`, so
   map `response.status` to the sealed result, don't catch exceptions for flow
   control.
3. Repository impl using the **injected** shared client — relative URLs only
   (base URL + `/api/v1/` are injected). Never `HttpClient()` in a feature.
4. Koin: bind repo impl + `viewModel { }` in `di/AppModule.kt`. Platform
   pieces → `expect`/`actual` bound in both `PlatformModule.android.kt` and
   `PlatformModule.ios.kt` (missing one compiles and crashes on that platform).
5. Route: `@Serializable` object/class in `core/navigation/Routes.kt` — params
   are primitives, screens re-fetch by id.
6. `App.kt`: `composable<FooRoute> { }` block; args via
   `backStackEntry.toRoute()`. Tab destination → also `TopLevelDestination`;
   tab screens need `paddingBottom = 80.dp` above the pill nav.
7. Strings: every user-visible string = 4 edits — `AppStrings` interface +
   `KoStrings` + `EnStrings` + `DeStrings`. (ViewModel-internal errors:
   hardcoded Korean, matching `AttendanceViewModel`.)
8. Screen per `designs/dn_app/DESIGN.md`: theme tokens only, surface-shift
   separators (no 1px lines), `spring()` everything, press = scale 0.97,
   chevron back + swipe both pop. New screen → run
   `lazyweb:lazyweb-quick-references` first.
9. Tests: hand-written fakes (NO mocking library exists — do not add one),
   `MockEngine` for the repo impl, VM test on `StandardTestDispatcher`.
   Test names: letters/digits/spaces only — punctuation breaks Kotlin/Native.

## House pattern (compressed from features/events + attendance)

```kotlin
// ViewModel — one immutable state, update{} + copy, viewModelScope
class FooViewModel(private val repository: FooRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FooUiState())
    val uiState: StateFlow<FooUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getFoos().fold(
                onSuccess = { foos -> _uiState.update { it.copy(isLoading = false, foos = foos) } },
                onFailure = { _uiState.update { it.copy(isLoading = false, error = "불러오지 못했습니다") } },
            )
        }
    }
}

// Test — StandardTestDispatcher + fake, kotlin.test asserts
@OptIn(ExperimentalCoroutinesApi::class)
class FooViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test fun `load exposes foos on success`() = runTest {
        val vm = FooViewModel(FakeFooRepository(foos = listOf(sampleFoo)))
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.foos.size)
    }
}
```

## Before you claim done

Run the ladder in **verifying-kmp-changes** (REQUIRED SUB-SKILL). Then update
the feature's row in `../dn-app/MVP.md` (status only, never delete rows).

## Common mistakes

| Mistake | Consequence |
|---|---|
| New pattern instead of the events/attendance shape | Review reject; the codebase stays learnable only if slices rhyme |
| `@Serializable` on domain models | Wire contract bleeds into domain; DTO changes ripple |
| Koin binding forgotten (or one platform module only) | Compiles, crashes at screen-open with "no definition found" |
| String route (`navigate("foo/$id")`) | Bypasses the type-safe route system |
| Hardcoded Korean in a composable | Fails the KO/EN/DE contract |
| External API called with the shared client's default host logic | Bearer token leaks to a foreign host — absolute URL + no-auth review required |
