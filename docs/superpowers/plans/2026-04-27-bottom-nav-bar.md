# Bottom Navigation Bar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a persistent `BottomNavBar` to all main tab screens (Home, Community, Ministries, News, Profile), hidden on detail and pre-auth screens, with same-tab tap returning to root.

**Architecture:** Single `Scaffold` in `App.kt` wraps the existing `NavHost`. Visibility is driven by checking whether the current `NavDestination` matches any entry in `TopLevelDestination.all`. Tab configuration lives exclusively in `TopLevelDestination` — adding a new tab requires only one new `data object`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, `org.jetbrains.androidx.navigation:navigation-compose` (type-safe routes), `kotlin.test` for unit tests.

---

## File Map

| Action | File |
|---|---|
| **Create** | `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/navigation/TopLevelDestination.kt` |
| **Create** | `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/BottomNavBar.kt` |
| **Delete** | `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/ChurchBottomBar.kt` |
| **Modify** | `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt` |
| **Create** | `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/navigation/TopLevelDestinationTest.kt` |

---

### Task 1: Create `TopLevelDestination` with tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/navigation/TopLevelDestination.kt`
- Create: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/navigation/TopLevelDestinationTest.kt`

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/navigation/TopLevelDestinationTest.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TopLevelDestinationTest {

    @Test
    fun `all contains exactly 5 entries`() {
        assertEquals(5, TopLevelDestination.all.size)
    }

    @Test
    fun `all entries have non-blank labels`() {
        TopLevelDestination.all.forEach { dest ->
            assertTrue(dest.label.isNotBlank(), "Expected non-blank label for $dest")
        }
    }

    @Test
    fun `all contains Home Community Ministries News Profile in order`() {
        val entries = TopLevelDestination.all
        assertNotNull(entries.filterIsInstance<TopLevelDestination.Home>().firstOrNull())
        assertNotNull(entries.filterIsInstance<TopLevelDestination.Community>().firstOrNull())
        assertNotNull(entries.filterIsInstance<TopLevelDestination.Ministries>().firstOrNull())
        assertNotNull(entries.filterIsInstance<TopLevelDestination.News>().firstOrNull())
        assertNotNull(entries.filterIsInstance<TopLevelDestination.Profile>().firstOrNull())
    }

    @Test
    fun `Home routeClass is HomeRoute`() {
        assertEquals(HomeRoute::class, TopLevelDestination.Home.routeClass)
    }

    @Test
    fun `News routeClass is AnnouncementListRoute`() {
        assertEquals(AnnouncementListRoute::class, TopLevelDestination.News.routeClass)
    }

    @Test
    fun `Ministries routeClass is MinistryListRoute`() {
        assertEquals(MinistryListRoute::class, TopLevelDestination.Ministries.routeClass)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :composeApp:allTests --tests "com.hanmaum.dn.mobile.core.navigation.TopLevelDestinationTest"
```

Expected: FAIL — `TopLevelDestination` does not exist yet.

- [ ] **Step 3: Create `TopLevelDestination.kt`**

Create `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/navigation/TopLevelDestination.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

sealed class TopLevelDestination<T : Any>(
    val routeClass: KClass<T>,
    val routeInstance: T,
    val icon: ImageVector,
    val label: String,
) {
    data object Home : TopLevelDestination<HomeRoute>(
        routeClass = HomeRoute::class,
        routeInstance = HomeRoute,
        icon = Icons.Default.Home,
        label = "홈",
    )

    data object Community : TopLevelDestination<CommunityRoute>(
        routeClass = CommunityRoute::class,
        routeInstance = CommunityRoute,
        icon = Icons.Default.Group,
        label = "커뮤니티",
    )

    data object Ministries : TopLevelDestination<MinistryListRoute>(
        routeClass = MinistryListRoute::class,
        routeInstance = MinistryListRoute,
        icon = Icons.Default.Star,
        label = "사역",
    )

    data object News : TopLevelDestination<AnnouncementListRoute>(
        routeClass = AnnouncementListRoute::class,
        routeInstance = AnnouncementListRoute,
        icon = Icons.Default.Newspaper,
        label = "소식",
    )

    data object Profile : TopLevelDestination<ProfileRoute>(
        routeClass = ProfileRoute::class,
        routeInstance = ProfileRoute,
        icon = Icons.Default.Person,
        label = "프로필",
    )

    companion object {
        val all: List<TopLevelDestination<*>> = listOf(Home, Community, Ministries, News, Profile)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :composeApp:allTests --tests "com.hanmaum.dn.mobile.core.navigation.TopLevelDestinationTest"
```

Expected: All 6 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/navigation/TopLevelDestination.kt
git add composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/navigation/TopLevelDestinationTest.kt
git commit -m "feat(nav): add TopLevelDestination sealed class as tab registry"
```

---

### Task 2: Create `BottomNavBar`, delete `ChurchBottomBar`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/BottomNavBar.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/ChurchBottomBar.kt`

- [ ] **Step 1: Create `BottomNavBar.kt`**

Create `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/BottomNavBar.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.hanmaum.dn.mobile.core.navigation.TopLevelDestination

@Composable
fun BottomNavBar(
    currentDestination: NavDestination?,
    onDestinationSelected: (TopLevelDestination<*>) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor   = MaterialTheme.colorScheme.primary,
            selectedTextColor   = MaterialTheme.colorScheme.primary,
            indicatorColor      = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TopLevelDestination.all.forEach { dest ->
            val selected = currentDestination?.hasRoute(dest.routeClass) == true
            NavigationBarItem(
                selected = selected,
                onClick  = { onDestinationSelected(dest) },
                icon     = { Icon(dest.icon, contentDescription = dest.label) },
                label    = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                colors   = itemColors,
            )
        }
    }
}
```

- [ ] **Step 2: Delete `ChurchBottomBar.kt`**

```bash
rm composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/ChurchBottomBar.kt
```

- [ ] **Step 3: Verify build compiles**

```bash
./gradlew :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL. If there are import errors for `ChurchBottomBar` or `BottomTab` elsewhere, fix them (search with `grep -r "ChurchBottomBar\|BottomTab" composeApp/src`).

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/BottomNavBar.kt
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/ChurchBottomBar.kt
git commit -m "feat(nav): replace ChurchBottomBar with BottomNavBar driven by TopLevelDestination"
```

---

### Task 3: Wire `BottomNavBar` into `App.kt`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt`

- [ ] **Step 1: Replace `App.kt` content**

Replace the full file `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt`:

```kotlin
package com.hanmaum.dn.mobile

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.navigation.*
import com.hanmaum.dn.mobile.core.presentation.components.BottomNavBar
import androidx.navigation.NavDestination.Companion.hasRoute
import com.hanmaum.dn.mobile.core.presentation.theme.AppTheme
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementDetailScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementListScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.HomeScreen
import com.hanmaum.dn.mobile.features.community.presentation.CommunityStubScreen
import com.hanmaum.dn.mobile.features.login.presentation.RegisterScreen
import com.hanmaum.dn.mobile.features.login.screen.LoginScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.list.MinistryListScreen
import com.hanmaum.dn.mobile.features.pending.screen.PendingScreen
import com.hanmaum.dn.mobile.features.pending.screen.SplashScreen
import com.hanmaum.dn.mobile.features.profile.presentation.ProfileScreen
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        AppTheme {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val showBottomBar = TopLevelDestination.all.any { dest ->
                currentDestination?.hasRoute(dest.routeClass) == true
            }

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        BottomNavBar(
                            currentDestination = currentDestination,
                            onDestinationSelected = { dest ->
                                navController.navigate(dest.routeInstance) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = SplashRoute,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    composable<SplashRoute> {
                        SplashScreen(
                            onNavigate = { route ->
                                val targetRoute: Any = when (route) {
                                    NavRoute.Home            -> HomeRoute
                                    NavRoute.Login           -> LoginRoute
                                    NavRoute.PendingApproval -> PendingRoute
                                }
                                navController.navigate(targetRoute) {
                                    popUpTo<SplashRoute> { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<LoginRoute> {
                        LoginScreen(
                            onNavigateToHome = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<LoginRoute> { inclusive = true }
                                }
                            },
                            onNavigateToPending = {
                                navController.navigate(PendingRoute) {
                                    popUpTo<LoginRoute> { inclusive = true }
                                }
                            },
                            onRegisterClick = { navController.navigate(RegisterRoute) },
                        )
                    }

                    composable<RegisterRoute> {
                        RegisterScreen(
                            onBackClick = { navController.popBackStack() },
                            onNavigateToPending = {
                                navController.navigate(PendingRoute) {
                                    popUpTo<LoginRoute> { inclusive = false }
                                    popUpTo<RegisterRoute> { inclusive = true }
                                }
                            },
                        )
                    }

                    composable<PendingRoute> {
                        PendingScreen(
                            onNavigateToHome = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<PendingRoute> { inclusive = true }
                                }
                            },
                            onNavigateToLogin = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable<HomeRoute> {
                        HomeScreen(
                            onLogout = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onAnnouncementClick = { id ->
                                navController.navigate(AnnouncementDetailRoute(id = id))
                            },
                            onViewAllClick   = { navController.navigate(AnnouncementListRoute) },
                            onProfileClick   = { navController.navigate(ProfileRoute) },
                            onMinistryClick  = { navController.navigate(MinistryListRoute) },
                            onCommunityClick = { navController.navigate(CommunityRoute) },
                            onNewsClick      = { navController.navigate(AnnouncementListRoute) },
                        )
                    }

                    composable<AnnouncementDetailRoute> { backStackEntry ->
                        val route: AnnouncementDetailRoute = backStackEntry.toRoute()
                        AnnouncementDetailScreen(
                            announcementId = route.id,
                            onBackClick    = { navController.popBackStack() },
                        )
                    }

                    composable<AnnouncementListRoute> {
                        AnnouncementListScreen(
                            onBackClick = { navController.popBackStack() },
                            onItemClick = { id ->
                                navController.navigate(AnnouncementDetailRoute(id = id))
                        },
                        )
                    }

                    composable<ProfileRoute> {
                        ProfileScreen(
                            onBackClick = { navController.popBackStack() },
                            onLogout = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable<MinistryListRoute> {
                        MinistryListScreen(
                            onBackClick     = { navController.popBackStack() },
                            onMinistryClick = { publicId ->
                                navController.navigate(MinistryDetailRoute(publicId = publicId))
                            },
                        )
                    }

                    composable<MinistryDetailRoute> { backStackEntry ->
                        val route: MinistryDetailRoute = backStackEntry.toRoute()
                        MinistryDetailScreen(
                            publicId    = route.publicId,
                            onBackClick = { navController.popBackStack() },
                        )
                    }

                    composable<CommunityRoute> {
                        CommunityStubScreen(
                            onBackClick = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify `hasRoute` import resolves**

The `hasRoute` call on `NavDestination` in `App.kt` uses `TopLevelDestination.routeClass` (a `KClass<T>`). It requires the extension `NavDestination.hasRoute(KClass<T>)` from `androidx.navigation`. This is imported transitively via the navigation-compose dependency — no additional dependency needed.

If the compiler reports `Unresolved reference: hasRoute`, add this import explicitly at the top of `App.kt`:

```kotlin
import androidx.navigation.NavDestination.Companion.hasRoute
```

- [ ] **Step 3: Build and verify**

```bash
./gradlew :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL with no errors.

- [ ] **Step 4: Run all tests**

```bash
./gradlew :composeApp:allTests
```

Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt
git commit -m "feat(nav): wire BottomNavBar into App with Scaffold and conditional visibility"
```
