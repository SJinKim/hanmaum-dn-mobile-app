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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.domain.repository.RememberedWeeklyVerse
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.features.verse.domain.model.DailyVerse
import com.hanmaum.dn.mobile.features.verse.domain.model.DailyVerseState
import com.hanmaum.dn.mobile.features.verse.domain.model.VerseRecordKind
import com.hanmaum.dn.mobile.features.verse.domain.model.VerseStreak
import com.hanmaum.dn.mobile.features.verse.domain.model.WeeklyVerse
import com.hanmaum.dn.mobile.features.verse.presentation.components.StreakBar
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
 * Everything on this screen is driven by endpoints the app already calls,
 * except the explicitly marked verse placeholders.
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
    LifecycleResumeEffect(attendanceViewModel) {
        attendanceViewModel.onResume()
        onPauseOrDispose { }
    }

    val uriHandler = LocalUriHandler.current
    val strings = LocalStrings.current
    val today = remember {
        kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

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

            // Hidden on days the plan has no entry, and while the call is in
            // flight — an empty passage card is worse than none.
            state.dailyVerse?.let { verse ->
                DailyPassageCard(
                    verse = verse,
                    streak = state.verseRecords?.quietTime,
                    streakError = state.verseRecordsError,
                    today = today,
                    onReadClick = { verse.sourceUrl?.let(uriHandler::openUri) },
                    onMarkToday = { viewModel.markVerseRecord(VerseRecordKind.QUIET_TIME) },
                )

                Spacer(Modifier.height(12.dp))
            }

            // Never hidden: with the verse, or with the reason there is none plus
            // whatever this device still remembers.
            WeeklyVerseCard(
                verse = state.weeklyVerse,
                error = state.weeklyVerseError,
                loaded = state.weeklyVerseLoaded,
                remembered = state.rememberedWeeklyVerse,
                streak = state.verseRecords?.recitation,
                streakError = state.verseRecordsError,
                today = today,
                onMarkToday = { viewModel.markVerseRecord(VerseRecordKind.RECITATION) },
            )

            // room for the floating dock plus its scroll edge
            Spacer(Modifier.height(DnDock.contentInset(extra = 22.dp)))
        }

        DnScrollEdge()
    }
}

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

/**
 * Today's passage: reference only, plus a way out to the church's reading page.
 *
 * A quiet-time passage runs 8–25 verses, so the full text does not belong on
 * Home — the card names the passage and hands the reading off. Korean and
 * English sit under each other regardless of app language, because the passage
 * is announced in Korean and read by members who search for it in English.
 */
@Composable
private fun DailyPassageCard(
    verse: DailyVerse,
    streak: VerseStreak?,
    streakError: String?,
    today: LocalDate,
    onReadClick: () -> Unit,
    onMarkToday: () -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val readable = verse.sourceUrl != null

    val readInteraction = remember { MutableInteractionSource() }
    val readPressed by readInteraction.collectIsPressedAsState()
    val readScale by animateFloatAsState(
        targetValue = if (readPressed) 0.97f else 1f,
        animationSpec = spring(),
        label = "readPress",
    )

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(DnCardShape)
            .background(c.surface, DnCardShape)
            .border(1.dp, c.strokeSubtle, DnCardShape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 읽기 sits up here beside the eyebrow rather than in a footer: the row
        // below it belongs to the streak now. The card itself is not clickable —
        // two targets on one surface would fight over a tap.
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(DnIcons.Book, null, tint = c.amber, modifier = Modifier.size(16.dp))
                Text(strings.verseTodayTitle, style = DnTheme.typography.label, color = c.amber)
            }

            if (readable) {
                Row(
                    Modifier
                        .scale(readScale)
                        .clickable(
                            interactionSource = readInteraction,
                            indication = null,
                            onClick = onReadClick,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        strings.verseReadAction,
                        style = DnTheme.typography.captionStrong,
                        color = c.amber,
                    )
                    Icon(DnIcons.ArrowUpRight, null, tint = c.amber, modifier = Modifier.size(14.dp))
                }
            }
        }

        when (verse.state) {
            // Sunday has no planned passage — the verses come from the sermon —
            // so the card names the service instead of a reference. The wording
            // lives here, not on the server, which is not localised.
            DailyVerseState.SUNDAY_SERVICE -> {
                Text(
                    strings.verseSundayServiceTitle,
                    style = DnTheme.typography.title,
                    color = c.textPrimary,
                )
                Text(
                    strings.verseSundayServiceHint,
                    style = DnTheme.typography.body,
                    color = c.textSecondary,
                )
            }

            // Either half can be empty if the server could not resolve that
            // language; the other one still carries the card.
            else -> {
                if (verse.referenceKo.isNotEmpty()) {
                    Text(verse.referenceKo, style = DnTheme.typography.title, color = c.textPrimary)
                }
                if (verse.referenceEn.isNotEmpty()) {
                    Text(verse.referenceEn, style = DnTheme.typography.body, color = c.textSecondary)
                }
            }
        }

        streak?.let {
            StreakBar(streak = it, today = today, onMarkToday = onMarkToday)
        }

        // No bar and a known reason: say so rather than leave a gap that looks
        // like nothing has been recorded yet.
        if (streak == null && streakError != null) {
            Text(
                strings.verseRecordsUnavailable(streakError),
                style = DnTheme.typography.caption,
                color = c.textTertiary,
            )
        }
    }
}

/**
 * 주간 암송 구절 — on screen in every case.
 *
 * It used to disappear whenever [verse] was null, which covered three very
 * different situations: nothing published, the church's source unreachable, and
 * a call still in flight. A member saw the same nothing for all of them.
 *
 * Now the card always stands, and only its middle changes. The amber fill is
 * reserved for the case where a verse is actually there — an amber card whose
 * whole message is "there is nothing" shouts for its content.
 */
@Composable
private fun WeeklyVerseCard(
    verse: WeeklyVerse?,
    error: String?,
    loaded: Boolean,
    remembered: RememberedWeeklyVerse?,
    streak: VerseStreak?,
    streakError: String?,
    today: LocalDate,
    onMarkToday: () -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val filled = verse != null

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
            Icon(DnIcons.Sparkle, null, tint = c.amber, modifier = Modifier.size(16.dp))
            Text(strings.verseWeeklyTitle, style = DnTheme.typography.label, color = c.amber)
        }

        if (verse != null) {
            Text(verse.text, style = DnTheme.typography.bodyStrong, color = c.textPrimary)
            VerseFootRow(verse.reference, weekRangeLabel(verse.weekStart, verse.weekEnd))
        } else {
            // Nothing is claimed while the call is still out: "nothing published"
            // would be a statement we cannot make yet, and it would flash away a
            // moment later.
            if (loaded) {
                Text(
                    if (error != null) strings.verseWeeklyUnavailable else strings.verseWeeklyEmpty,
                    style = DnTheme.typography.body,
                    color = c.textSecondary,
                )
            }
            // The server is out of answers here, so this is the only source left.
            // Null on a device that has never loaded a verse.
            remembered?.let {
                VerseFootRow(
                    reference = strings.verseWeeklyRemembered(it.reference),
                    weekRange = weekRangeLabel(it.weekStart, it.weekEnd),
                )
            }
        }

        streak?.let {
            StreakBar(streak = it, today = today, onMarkToday = onMarkToday)
        }

        // Same as the passage card: a missing bar with a known reason says so.
        if (streak == null && streakError != null) {
            Text(
                strings.verseRecordsUnavailable(streakError),
                style = DnTheme.typography.caption,
                color = c.textTertiary,
            )
        }
    }
}

/** Reference on the left, the week it belongs to on the right. */
@Composable
private fun VerseFootRow(reference: String, weekRange: String?) {
    val c = DnTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(reference, style = DnTheme.typography.caption, color = c.textTertiary)
        weekRange?.let {
            Text(it, style = DnTheme.typography.caption, color = c.textTertiary)
        }
    }
}

/** Null when the server sent no span — then the row carries the reference alone. */
@Composable
private fun weekRangeLabel(start: LocalDate?, end: LocalDate?): String? {
    if (start == null || end == null) return null
    return LocalStrings.current.verseWeekRange(
        startMonth = start.month.ordinal + 1,
        startDay = start.day,
        endMonth = end.month.ordinal + 1,
        endDay = end.day,
    )
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
