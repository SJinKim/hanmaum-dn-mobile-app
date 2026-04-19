# Home Screen Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign `HomeScreen` and its components to match the Luminous Sanctuary screenshots — gradient hero pager, Bible verse block, weekly memory verse dark card, and a 5-tab bottom nav wired to Community (stub) and News routes.

**Architecture:** `HomeScreen.kt` hosts a custom top bar, then delegates to three section composables (`HeroBannerSection`, `BibleVerseSection`, `WeeklyVerseSection`) followed by the existing `LatestNewsSection`. `ChurchBottomBar` is expanded from 4 to 5 tabs. A `CommunityStubScreen` is added and all new routes wired in `App.kt`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material3, `androidx.compose.foundation.pager.HorizontalPager`, Koin, type-safe Navigation (kotlinx-serialization routes)

---

## File Map

| Action | File |
|--------|------|
| Modify | `core/presentation/components/ChurchBottomBar.kt` |
| Modify | `core/navigation/Routes.kt` |
| Create | `features/community/presentation/CommunityStubScreen.kt` |
| Modify | `features/announcement/presentation/components/HeroBannerSection.kt` |
| Create | `features/announcement/presentation/components/BibleVerseSection.kt` |
| Create | `features/announcement/presentation/components/WeeklyVerseSection.kt` |
| Modify | `features/announcement/presentation/HomeScreen.kt` |
| Modify | `App.kt` |
| Delete | `features/announcement/presentation/components/HeroBannerSection2.kt` |
| Delete | `features/announcement/presentation/components/QuickMenuSection.kt` |

---

## Task 1: Expand `BottomTab` enum and rebuild `ChurchBottomBar` with 5 tabs

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/ChurchBottomBar.kt`

- [ ] **Step 1: Replace the entire file content**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class BottomTab { HOME, COMMUNITY, MINISTRIES, NEWS, PROFILE }

@Composable
fun ChurchBottomBar(
    selectedTab: BottomTab = BottomTab.HOME,
    onTabSelected: (BottomTab) -> Unit = {},
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

        NavigationBarItem(
            selected = selectedTab == BottomTab.HOME,
            onClick  = { onTabSelected(BottomTab.HOME) },
            icon     = { Icon(Icons.Default.Home, contentDescription = "홈") },
            label    = { Text("홈", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.COMMUNITY,
            onClick  = { onTabSelected(BottomTab.COMMUNITY) },
            icon     = { Icon(Icons.Default.Group, contentDescription = "커뮤니티") },
            label    = { Text("커뮤니티", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.MINISTRIES,
            onClick  = { onTabSelected(BottomTab.MINISTRIES) },
            icon     = { Icon(Icons.Default.Star, contentDescription = "사역") },
            label    = { Text("사역", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.NEWS,
            onClick  = { onTabSelected(BottomTab.NEWS) },
            icon     = { Icon(Icons.Default.Newspaper, contentDescription = "소식") },
            label    = { Text("소식", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.PROFILE,
            onClick  = { onTabSelected(BottomTab.PROFILE) },
            icon     = { Icon(Icons.Default.Person, contentDescription = "프로필") },
            label    = { Text("프로필", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/ChurchBottomBar.kt
git commit -m "feat(nav): expand bottom nav to 5 tabs — Community, Ministries, News added"
```

---

## Task 2: Add `CommunityRoute` to `Routes.kt` and create `CommunityStubScreen`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/navigation/Routes.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/community/presentation/CommunityStubScreen.kt`

- [ ] **Step 1: Add `CommunityRoute` to `Routes.kt`**

Append after the last route object:

```kotlin
@Serializable
object CommunityRoute
```

Full file after edit:

```kotlin
package com.hanmaum.dn.mobile.core.navigation

import kotlinx.serialization.Serializable

@Serializable object SplashRoute
@Serializable object LoginRoute
@Serializable object RegisterRoute
@Serializable object PendingRoute
@Serializable object HomeRoute
@Serializable object AnnouncementListRoute
@Serializable data class AnnouncementDetailRoute(val id: String)
@Serializable object ProfileRoute
@Serializable object MinistryListRoute
@Serializable data class MinistryDetailRoute(val publicId: String)
@Serializable object CommunityRoute
```

- [ ] **Step 2: Create `CommunityStubScreen.kt`**

```kotlin
package com.hanmaum.dn.mobile.features.community.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hanmaum.dn.mobile.core.presentation.components.ChurchTopBar

@Composable
fun CommunityStubScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = { ChurchTopBar(title = "커뮤니티", onBackClick = onBackClick) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text("준비 중입니다")
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/navigation/Routes.kt
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/community/presentation/CommunityStubScreen.kt
git commit -m "feat(community): add CommunityRoute and stub screen"
```

---

## Task 3: Redesign `HeroBannerSection.kt` — gradient pager

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/HeroBannerSection.kt`

The existing file has a working `HorizontalPager` + `AutoScrollEffect` — keep that infrastructure, replace the card visuals entirely.

- [ ] **Step 1: Replace the entire file content**

```kotlin
package com.hanmaum.dn.mobile.features.announcement.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanmaum.dn.mobile.core.presentation.theme.CardWhite
import com.hanmaum.dn.mobile.core.presentation.theme.CoralDark
import com.hanmaum.dn.mobile.core.presentation.theme.MutedGray
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroBannerSection(
    banners: List<Announcement>,
    onBannerClick: (String) -> Unit,
) {
    if (banners.isEmpty()) {
        HeroBannerLoading()
        return
    }

    val items = banners.take(5)
    val initialPage = (Int.MAX_VALUE / 2 / items.size) * items.size
    val pagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }

    AutoScrollEffect(pagerState = pagerState, itemCount = items.size)

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
        ) { pageIndex ->
            val item = items[pageIndex % items.size]
            HeroBannerCard(
                announcement = item,
                onClick = { onBannerClick(item.id) },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        HeroBannerIndicators(
            totalCount = items.size,
            currentIndex = pagerState.currentPage % items.size,
        )
    }
}

@Composable
private fun HeroBannerCard(
    announcement: Announcement,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(CoralDark, Color(0xFF1A0A0A))
                )
            )
            .clickable(onClick = onClick)
            .padding(24.dp),
    ) {
        // Eyebrow
        Text(
            text = "DN App",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.TopStart),
        )

        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Sermon title
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).sp,
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Service pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "주일 예배  •  ${announcement.startAt.take(10)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
            }

            // CTA button
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardWhite,
                    contentColor   = CoralDark,
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "예배 공지 읽기",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun HeroBannerIndicators(
    totalCount: Int,
    currentIndex: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(totalCount) { i ->
            val isSelected = i == currentIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) CoralDark
                        else MutedGray.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

@Composable
private fun HeroBannerLoading() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(CoralDark, Color(0xFF1A0A0A))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AutoScrollEffect(pagerState: PagerState, itemCount: Int) {
    if (itemCount > 1) {
        LaunchedEffect(pagerState, itemCount) {
            while (true) {
                delay(4000)
                try {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                } catch (_: Exception) {}
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/HeroBannerSection.kt
git commit -m "feat(home): redesign hero banner — gradient pager with 5-item auto-scroll"
```

---

## Task 4: Create `BibleVerseSection.kt`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/BibleVerseSection.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.hanmaum.dn.mobile.features.announcement.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanmaum.dn.mobile.core.presentation.theme.MutedGray
import com.hanmaum.dn.mobile.core.presentation.theme.SoftPeach
import com.hanmaum.dn.mobile.core.presentation.theme.WarmCharcoal

@Composable
fun BibleVerseSection(onViewAllClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftPeach)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "오늘의 말씀",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.05.sp,
            ),
            color = MutedGray,
        )

        Text(
            text = "\"빛이 어둠에 비치되 어둠이 깨닫지 못하더라\"",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
            color = WarmCharcoal,
        )

        Text(
            text = "요한복음 1:5",
            style = MaterialTheme.typography.labelMedium,
            color = MutedGray,
        )

        TextButton(
            onClick = onViewAllClick,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                text = "확인하기 →",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/BibleVerseSection.kt
git commit -m "feat(home): add BibleVerseSection with SoftPeach background"
```

---

## Task 5: Create `WeeklyVerseSection.kt`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/WeeklyVerseSection.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.hanmaum.dn.mobile.features.announcement.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanmaum.dn.mobile.core.presentation.theme.DeepCharcoal
import com.hanmaum.dn.mobile.core.presentation.theme.GoldLight
import com.hanmaum.dn.mobile.core.presentation.theme.MutedGray
import com.hanmaum.dn.mobile.core.presentation.theme.SanctuaryWhite

// TODO(api): Replace hardcoded strings with data from /api/v1/verses/weekly
// wired through VerseRepository → HomeViewModel in a future session.
@Composable
fun WeeklyVerseSection() {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DeepCharcoal)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "주간 암송 구절",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.05.sp,
            ),
            color = GoldLight,
        )

        Text(
            text = "여호와는 나의 목자시니 내게 부족함이 없으리로다",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = SanctuaryWhite,
        )

        Text(
            text = "시편 23:1",
            style = MaterialTheme.typography.labelMedium,
            color = MutedGray,
        )

        TextButton(
            onClick = { /* TODO: 암송 기능 추가 예정 */ },
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                text = "외우기 시작 →",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = GoldLight,
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/WeeklyVerseSection.kt
git commit -m "feat(home): add WeeklyVerseSection dark card with hardcoded verse"
```

---

## Task 6: Redesign `HomeScreen.kt` — new top bar, wire sections, update callbacks

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/HomeScreen.kt`

- [ ] **Step 1: Replace the entire file content**

```kotlin
package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.presentation.components.BottomTab
import com.hanmaum.dn.mobile.core.presentation.components.ChurchBottomBar
import com.hanmaum.dn.mobile.core.presentation.components.ErrorView
import com.hanmaum.dn.mobile.features.announcement.presentation.components.BibleVerseSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.HeroBannerSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.LatestNewsSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.WeeklyVerseSection
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onAnnouncementClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onProfileClick: () -> Unit,
    onMinistryClick: () -> Unit,
    onCommunityClick: () -> Unit,
    onNewsClick: () -> Unit,
) {
    val viewModel: HomeViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HomeTopBar() },
        bottomBar = {
            ChurchBottomBar(
                selectedTab = BottomTab.HOME,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomTab.PROFILE    -> onProfileClick()
                        BottomTab.MINISTRIES -> onMinistryClick()
                        BottomTab.COMMUNITY  -> onCommunityClick()
                        BottomTab.NEWS       -> onNewsClick()
                        else                 -> {}
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                state.error != null -> ErrorView(
                    msg = state.error,
                    onRetry = { viewModel.loadAnnouncements() },
                )
                else -> HomeContent(
                    state = state,
                    onAnnouncementClick = onAnnouncementClick,
                    onViewAllClick = onViewAllClick,
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "DN App",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        IconButton(onClick = { /* 알림 기능 추가 예정 */ }) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "알림",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onAnnouncementClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HeroBannerSection(
            banners = state.banners,
            onBannerClick = onAnnouncementClick,
        )

        Spacer(modifier = Modifier.height(8.dp))

        BibleVerseSection(onViewAllClick = onViewAllClick)

        Spacer(modifier = Modifier.height(24.dp))

        WeeklyVerseSection()

        Spacer(modifier = Modifier.height(24.dp))

        LatestNewsSection(
            newsList = state.announcements,
            onItemClick = onAnnouncementClick,
            onViewAllClick = onViewAllClick,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/HomeScreen.kt
git commit -m "feat(home): redesign HomeScreen — new top bar, gradient hero pager, verse sections"
```

---

## Task 7: Wire new routes and callbacks in `App.kt`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt`

- [ ] **Step 1: Add imports and wire `HomeScreen`, `CommunityStubScreen` route**

Replace the full `App.kt` with:

```kotlin
package com.hanmaum.dn.mobile

import androidx.compose.runtime.*
import com.hanmaum.dn.mobile.core.presentation.theme.AppTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.navigation.*
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

            NavHost(
                navController = navController,
                startDestination = SplashRoute,
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
                        onViewAllClick  = { navController.navigate(AnnouncementListRoute) },
                        onProfileClick  = { navController.navigate(ProfileRoute) },
                        onMinistryClick = { navController.navigate(MinistryListRoute) },
                        onCommunityClick = { navController.navigate(CommunityRoute) },
                        onNewsClick     = { navController.navigate(AnnouncementListRoute) },
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
                        onBackClick   = { navController.popBackStack() },
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
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt
git commit -m "feat(nav): wire CommunityRoute and News tab to AnnouncementListRoute in App.kt"
```

---

## Task 8: Delete unused files

**Files:**
- Delete: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/HeroBannerSection2.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/QuickMenuSection.kt`

- [ ] **Step 1: Delete both files and commit**

```bash
git rm composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/HeroBannerSection2.kt
git rm composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/QuickMenuSection.kt
git commit -m "chore: remove HeroBannerSection2 (unused) and QuickMenuSection (replaced by bottom nav)"
```

---

## Task 9: Build and verify

- [ ] **Step 1: Build the Android debug APK**

Run from `HanmaumDnApp/`:
```bash
./gradlew :composeApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: If build fails on icon reference**

`Icons.Default.Newspaper` may not be in the default icon set on all Compose versions. If you see an unresolved reference, replace it with `Icons.AutoMirrored.Filled.Article`:

In `ChurchBottomBar.kt`, change:
```kotlin
import androidx.compose.material.icons.filled.Newspaper
// ...
Icon(Icons.Default.Newspaper, contentDescription = "소식")
```
to:
```kotlin
import androidx.compose.material.icons.automirrored.filled.Article
// ...
Icon(Icons.AutoMirrored.Filled.Article, contentDescription = "소식")
```

Then rebuild.

- [ ] **Step 3: Commit fix if applied**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/ChurchBottomBar.kt
git commit -m "fix(nav): use AutoMirrored.Article icon for News tab"
```
