package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlassIconButton
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnImagePlaceholder
import com.hanmaum.dn.mobile.core.presentation.components.DnDock
import com.hanmaum.dn.mobile.core.presentation.components.DnScrollEdge
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnPillShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceSummary
import com.hanmaum.dn.mobile.features.attendance.presentation.AttendanceViewModel
import com.hanmaum.dn.mobile.features.attendance.presentation.components.SlideToCheckIn
import org.koin.compose.viewmodel.koinViewModel

/**
 * Home.
 *
 * Everything on this screen is driven by endpoints the app already calls —
 * announcements and the attendance definition. The two places the backend
 * cannot fill yet (the unread badge and the attendance summary) are marked
 * with the issue that tracks them and render placeholder content so the
 * layout can still be judged.
 */
@Composable
fun HomeScreen(
    onAnnouncementClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onFloorPlanClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onNurtureClick: () -> Unit,
    onServeClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onCommunityClick: () -> Unit,
) {
    val viewModel: HomeViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Nothing else asks for the content: the ViewModel's init only wires up the
    // push-token collector. Without this, banners, announcements and the
    // greeting name all stay empty for ever.
    //
    // On entry rather than on resume, which is the split loadUnseenCount()
    // documents: the badge is cheap and goes stale the moment the notification
    // list opens, the whole list is neither.
    LaunchedEffect(Unit) { viewModel.loadAnnouncements() }

    // The badge is cleared server-side the moment the list opens, so it has to
    // be re-read on the way back — otherwise Home keeps showing a stale count.
    LifecycleResumeEffect(Unit) {
        viewModel.loadUnseenCount()
        onPauseOrDispose { }
    }

    val attendanceViewModel: AttendanceViewModel = koinViewModel()
    val attendance by attendanceViewModel.uiState.collectAsStateWithLifecycle()

    val c = DnTheme.colors

    DnBackground(glows = DnGlows.action()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            HomeHeader(
                memberName = state.memberName,
                onProfileClick = onProfileClick,
                onNotificationsClick = onNotificationsClick,
                unseenCount = state.unseenCount,
            )

            Spacer(Modifier.height(14.dp))

            QuickMenuRow(
                onNurtureClick = onNurtureClick,
                onServeClick = onServeClick,
                onFloorPlanClick = onFloorPlanClick,
                onAttendanceClick = onAttendanceClick,
                onCommunityClick = onCommunityClick,
            )

            Spacer(Modifier.height(20.dp))

            NewsCarousel(
                banners = state.banners.ifEmpty { state.announcements },
                onClick = onAnnouncementClick,
            )

            Spacer(Modifier.height(20.dp))

            Box(Modifier.padding(horizontal = 20.dp)) {
                SlideToCheckIn(
                    label = if (attendance.isCheckedIn) "출석 완료" else "밀어서 출석하기",
                    disabledLabel = "출석 시간이 아닙니다",
                    enabled = attendance.isInWindow && !attendance.isCheckedIn,
                    checkedIn = attendance.isCheckedIn,
                    isBusy = attendance.isCheckingIn,
                    onCheckIn = attendanceViewModel::checkIn,
                )
            }

            Spacer(Modifier.height(16.dp))

            HomeTiles(
                serviceTitle = attendance.definition?.title,
                serviceTime = attendance.definition?.let { def ->
                    "${dayLabel(def.dayOfWeek)} ${def.windowStart.take(5)}"
                },
                // Same AttendanceViewModel that drives the check-in slider above,
                // so the tile costs no second request.
                summary = attendance.summary,
                onAttendanceClick = onAttendanceClick,
            )

            Spacer(Modifier.height(12.dp))

            VerseCard(
                eyebrow = "오늘의 말씀",
                icon = DnIcons.Book,
                // TODO(hanmaum-dn-server#115): no verse endpoint yet
                verse = PLACEHOLDER_VERSE,
                reference = "본문 미정 · 자리표시자",
                filled = false,
            )

            Spacer(Modifier.height(12.dp))

            VerseCard(
                eyebrow = "주간 암송 구절",
                icon = DnIcons.Sparkle,
                // TODO(hanmaum-dn-server#115): no verse endpoint yet
                verse = PLACEHOLDER_VERSE_WEEKLY,
                reference = "본문 미정 · 자리표시자",
                filled = true,
            )

            // room for the floating dock plus its scroll edge
            Spacer(Modifier.height(DnDock.contentInset(extra = 22.dp)))
        }

        DnScrollEdge()
    }
}

/** Placeholder copy — replaced once the verse endpoint exists. */
private const val PLACEHOLDER_VERSE =
    "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor."

private const val PLACEHOLDER_VERSE_WEEKLY =
    "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore."

@Composable
private fun HomeHeader(
    memberName: String?,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    unseenCount: Int,
) {
    val c = DnTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // the avatar is how you reach the profile now that it left the dock
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(c.surface2)
                .border(1.5.dp, c.strokeStrong, RoundedCornerShape(percent = 50))
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(DnIcons.User, "프로필", tint = c.textPrimary, modifier = Modifier.size(22.dp))
        }

        Column(Modifier.weight(1f)) {
            Text("Welcome", style = DnTheme.typography.caption, color = c.textSecondary)
            Text(
                text = memberName ?: "…",
                style = DnTheme.typography.headline,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box {
            DnGlassIconButton(DnIcons.Bell, "알림", onNotificationsClick)
            // GET /api/v1/me/notifications/unseen-count. No badge at zero — an
            // empty badge would promise something that isn't there.
            if (unseenCount > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(c.red)
                        .border(2.dp, c.canvas, RoundedCornerShape(percent = 50)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (unseenCount > 9) "9+" else unseenCount.toString(),
                        style = DnTheme.typography.label,
                        color = c.onRed,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickMenuRow(
    onNurtureClick: () -> Unit,
    onServeClick: () -> Unit,
    onFloorPlanClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onCommunityClick: () -> Unit,
) {
    val c = DnTheme.colors
    val entries = listOf(
        "양육" to onNurtureClick,
        "사역" to onServeClick,
        "교회 지도" to onFloorPlanClick,
        "출석 현황" to onAttendanceClick,
        "커뮤니티" to onCommunityClick,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(entries) { (label, action) ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(c.surface2, RoundedCornerShape(percent = 50))
                    .border(1.dp, c.strokeSubtle, RoundedCornerShape(percent = 50))
                    .clickable(onClick = action)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(label, style = DnTheme.typography.captionStrong, color = c.textPrimary)
            }
        }
    }
}

@Composable
private fun NewsCarousel(
    banners: List<Announcement>,
    onClick: (String) -> Unit,
) {
    val c = DnTheme.colors
    if (banners.isEmpty()) return

    val items = banners.take(5)
    val pagerState = rememberPagerState(pageCount = { items.size })

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // A pager rather than a free-scrolling row: one announcement at a time,
        // centred, and it snaps instead of coming to rest half off-screen. The
        // symmetric content padding is what centres a single card.
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp,
        ) { page ->
            val item = items[page]
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(DnCardShape)
                    .background(c.surface, DnCardShape)
                    .border(1.dp, c.strokeSubtle, DnCardShape)
                    .clickable { onClick(item.id) }
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // TODO(#111): AnnouncementDto carries imageUrl now; the client
                // does not map or render it yet.
                DnImagePlaceholder(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    cornerRadius = 18.dp,
                    label = "이미지",
                )
                CategoryPill(item)
                Text(
                    text = item.title,
                    style = DnTheme.typography.title,
                    color = c.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.startAt.take(10),
                    style = DnTheme.typography.caption,
                    color = c.textSecondary,
                )
            }
        }

        // Dots only earn their space when there is somewhere to swipe to.
        if (items.size > 1) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(items.size) { i ->
                    val active = pagerState.currentPage == i
                    Box(
                        Modifier
                            .height(6.dp)
                            .width(if (active) 18.dp else 6.dp)
                            .clip(DnPillShape)
                            .background(if (active) c.limeInk else c.strokeStrong)
                    )
                }
            }
        }
    }
}

/** Category colour follows the palette roles, not the legacy hex on the model. */
@Composable
internal fun CategoryPill(item: Announcement) {
    val c = DnTheme.colors
    val (container, ink) = when (item.category) {
        "NOTICE" -> c.blueDim to c.blue
        "MINISTRY" -> c.limeDim to c.limeInk
        "EVENT" -> c.amberDim to c.amber
        else -> c.surface2 to c.textSecondary
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(container, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(item.getAnnouncementCategoryName(), style = DnTheme.typography.label, color = ink)
    }
}

@Composable
private fun HomeTiles(
    serviceTitle: String?,
    serviceTime: String?,
    summary: AttendanceSummary?,
    onAttendanceClick: () -> Unit,
) {
    val c = DnTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .clip(DnTileShape)
                .background(c.surface, DnTileShape)
                .border(1.dp, c.strokeSubtle, DnTileShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("다음 예배", style = DnTheme.typography.caption, color = c.textSecondary)
                Icon(DnIcons.Clock, null, tint = c.blue, modifier = Modifier.size(16.dp))
            }
            Text(
                text = serviceTime ?: "예정 없음",
                style = DnTheme.typography.headline,
                color = c.textPrimary,
            )
            Text(
                text = serviceTitle ?: "오늘 예배가 없습니다",
                style = DnTheme.typography.caption,
                color = c.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            Modifier
                .weight(1f)
                .clip(DnTileShape)
                .background(c.surface, DnTileShape)
                .border(1.dp, c.strokeSubtle, DnTileShape)
                .clickable(onClick = onAttendanceClick)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("이번 달 출석", style = DnTheme.typography.caption, color = c.textSecondary)
                Icon(DnIcons.UserCheck, null, tint = c.limeInk, modifier = Modifier.size(16.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                // Dash while the summary is still in flight or failed — a zero
                // would read as "attended nothing this month".
                Text(
                    summary?.monthAttended?.toString() ?: "–",
                    style = DnTheme.typography.stat,
                    color = c.textPrimary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "/ ${summary?.monthTotal?.toString() ?: "–"}",
                    style = DnTheme.typography.body,
                    color = c.textTertiary,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.surface3)
            )
        }
    }
}

@Composable
private fun VerseCard(
    eyebrow: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    verse: String,
    reference: String,
    filled: Boolean,
) {
    val c = DnTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(DnCardShape)
            .background(if (filled) c.amberDim else c.surface, DnCardShape)
            .border(1.dp, if (filled) c.amberDim else c.strokeSubtle, DnCardShape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, null, tint = c.amber, modifier = Modifier.size(16.dp))
            Text(eyebrow, style = DnTheme.typography.label, color = c.amber)
        }
        Text(verse, style = DnTheme.typography.bodyStrong, color = c.textPrimary)
        Text(reference, style = DnTheme.typography.caption, color = c.textTertiary)
    }
}

private fun dayLabel(dayOfWeek: String): String = when (dayOfWeek.uppercase()) {
    "MONDAY" -> "월요일"
    "TUESDAY" -> "화요일"
    "WEDNESDAY" -> "수요일"
    "THURSDAY" -> "목요일"
    "FRIDAY" -> "금요일"
    "SATURDAY" -> "토요일"
    "SUNDAY" -> "일요일"
    else -> dayOfWeek
}
