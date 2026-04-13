# Home Screen Redesign — Design Spec

**Date:** 2026-04-13
**Branch target:** `feat/luminous-sanctuary-design`
**Scope:** HanmaumDnApp — HomeScreen visual redesign to match new screenshot designs

---

## 1. Overview

Redesign `HomeScreen.kt` and its child components to match the new Luminous Sanctuary design screenshots. All Korean text is preserved. No new network/image dependencies — hero uses a gradient background; S3 image URLs will be wired in a later session. Two new bottom nav tabs (Community, News) are wired up, with Community navigating to a stub screen.

---

## 2. Top Bar

Replace the current `ChurchTopBar` usage in `HomeScreen.kt` with a custom `Row`:

- **Left**: `"DN App"` in `MaterialTheme.typography.titleLarge`, color = `CoralDark` (primary)
- **Right**: `Icons.Default.Notifications` `IconButton` — no-op action (push notifications future feature)
- **Background**: `SanctuaryWhite`, no elevation, no border
- Implemented inline in `HomeScreen.kt` as a private `HomeTopBar` composable — not a reusable component since other screens use `ChurchTopBar`

---

## 3. Hero Banner (`HeroBannerSection.kt`, full redesign)

Full-width card replacing the current pager-based implementation.

**Layout:**
- Horizontal padding: `16.dp` from screen edges
- Height: `240.dp` fixed
- Corner radius: `24.dp`
- Background: vertical `Brush.verticalGradient(listOf(CoralDark, Color(0xFF1A0A0A)))`

**Content (top to bottom):**
1. Eyebrow label: `"DN App"` — `labelSmall`, `OnCoral` at 70% alpha
2. Sermon title: first banner announcement title — `headlineLarge`, `Color.White`, `letterSpacing = (-0.02).sp`
3. Service info pill: `"주일 예배  •  {date}"` — `surface` at 20% alpha background, `Color.White` text, `cornerRadius = 50.dp` (pill)
4. CTA button: `"예배 공지 읽기"` — pill-shaped, `CardWhite` background, `CoralDark` text, taps `onBannerClick(announcement.publicId)`

**Empty/loading state:** `Box` at same dimensions with gradient background + `CircularProgressIndicator` centered.

**Data source:** First item of `state.banners` (already loaded by `HomeViewModel`). Date formatted from `announcement.createdAt`.

---

## 4. Bible Verse Block (`BibleVerseSection.kt`, new)

Full-width section immediately below the hero.

- **Background**: `SoftPeach` (`#FFF5E1`) — no border, separation via background shift
- **Padding**: `24.dp` horizontal, `20.dp` vertical
- **Eyebrow**: `"오늘의 말씀"` — `labelSmall`, uppercase, `MutedGray`, `letterSpacing = 0.05.sp`
- **Verse**: `"빛이 어둠에 비치되 어둠이 깨닫지 못하더라"` — `bodyLarge`, `WarmCharcoal`, `fontStyle = FontStyle.Italic`
- **Reference**: `"요한복음 1:5"` — `labelMedium`, `MutedGray`
- **Link**: `"확인하기 →"` — `TextButton`, `secondary` color (Faith Blue), calls `onViewAllClick`

Content is hardcoded for now. A later session can make it API-driven.

---

## 5. Weekly Memory Verse (`WeeklyVerseSection.kt`, new)

Full-width dark card below the Bible verse block.

- **Margin**: `16.dp` horizontal
- **Background**: `DeepCharcoal` (`#2D3436`)
- **Corner radius**: `16.dp`
- **Padding**: `24.dp` all sides

**Content:**
1. Eyebrow: `"주간 암송 구절"` — `labelSmall`, uppercase, `GoldLight`, `letterSpacing = 0.05.sp`
2. Verse text: `"여호와는 나의 목자시니 내게 부족함이 없으리로다"` — `bodyLarge`, `SanctuaryWhite`
3. Reference: `"시편 23:1"` — `labelMedium`, `MutedGray`
4. Bottom link: `"외우기 시작 →"` — `TextButton`, `GoldLight` color, no-op for now

Content is hardcoded for now.

---

## 6. Bottom Nav (`ChurchBottomBar.kt` + `BottomTab` enum, updated)

### `BottomTab` enum — add 2 new values:

| Value | Label | Icon |
|---|---|---|
| `HOME` | `홈` | `Icons.Default.Home` |
| `COMMUNITY` | `커뮤니티` | `Icons.Default.Group` |
| `MINISTRIES` | `사역` | `Icons.Default.Star` |
| `NEWS` | `소식` | `Icons.Default.Article` (or `Icons.Default.FeedOutlined`) |
| `PROFILE` | `프로필` | `Icons.Default.Person` |

### `ChurchBottomBar.kt` — update to render all 5 tabs using the enum.

---

## 7. Community Stub Screen (`CommunityStubScreen.kt`, new)

Minimal placeholder for the Community tab.

- `Scaffold` with `ChurchTopBar(title = "커뮤니티")`
- Body: `Box(fillMaxSize)` with `Text("준비 중입니다", Modifier.align(Center))`
- Located at: `features/community/presentation/CommunityStubScreen.kt`

---

## 8. Routing (`Routes.kt` + `App.kt`, updated)

### `Routes.kt` — add:
```kotlin
@Serializable object CommunityRoute
```

### `App.kt` — add composable for `CommunityRoute → CommunityStubScreen`. Update `HomeScreen` callback:
- `NEWS` tab → `navController.navigate(AnnouncementListRoute)`
- `COMMUNITY` tab → `navController.navigate(CommunityRoute)`
- `MINISTRIES` tab → already wired

### `HomeScreen.kt` — pass `onCommunityClick` and `onNewsClick` callbacks down to `ChurchBottomBar`.

---

## 9. Files Changed

| File | Action |
|---|---|
| `features/announcement/presentation/HomeScreen.kt` | Redesign top bar, restructure `HomeContent`, add new callbacks |
| `features/announcement/presentation/components/HeroBannerSection.kt` | Full redesign |
| `features/announcement/presentation/components/BibleVerseSection.kt` | New |
| `features/announcement/presentation/components/WeeklyVerseSection.kt` | New |
| `core/presentation/components/ChurchBottomBar.kt` | Add COMMUNITY + NEWS tabs |
| `core/navigation/Routes.kt` | Add `CommunityRoute` |
| `features/community/presentation/CommunityStubScreen.kt` | New |
| `App.kt` | Wire new routes + tab callbacks |

`HeroBannerSection2.kt` and `QuickMenuSection.kt` — **deleted** (QuickMenu is replaced by the new hero layout; HeroBannerSection2 was unused).

---

## 10. Out of Scope

- Push notification bell action (future session)
- S3 image loading in hero (future session — swap gradient for `AsyncImage`)
- Bible verse / memory verse API integration (future session)
- Full Community screen implementation (last in the session sequence)
- Announcement list redesign (separate session)
