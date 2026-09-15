package com.hanmaum.dn.mobile.features.ministry.presentation.list

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnErrorState
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnScrollEdge
import com.hanmaum.dn.mobile.core.navigation.ParticipationRoute
import com.hanmaum.dn.mobile.core.presentation.components.DnSegmented
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.training.domain.model.Training
import com.hanmaum.dn.mobile.features.training.presentation.TrainingFormat
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureListSkeleton
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureMessageCard
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureOpenBadge
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureUnavailableCard
import com.hanmaum.dn.mobile.features.training.presentation.list.TrainingListUiState
import com.hanmaum.dn.mobile.features.training.presentation.list.TrainingListViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * 양육 · 사역 — two peer lists in one screen.
 *
 * Both answer the same question ("where can I join in?") and share the same
 * shape: browse, open, apply. Two separate screens would have doubled the
 * navigation without expressing a difference that exists.
 *
 * The title follows the active tab, as specified.
 */
@Composable
fun ParticipationScreen(
    initialTab: String,
    onBackClick: () -> Unit,
    onMinistryClick: (String) -> Unit,
    onNurtureClick: (String) -> Unit,
) {
    val viewModel: MinistryListViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val trainingViewModel: TrainingListViewModel = koinViewModel()
    val nurture by trainingViewModel.uiState.collectAsStateWithLifecycle()
    val c = DnTheme.colors

    // rememberSaveable, not remember: navigating into a detail takes this
    // screen out of composition, and a plain remember loses the tab. On the way
    // back it was then re-derived from the route argument, so a 양육 detail
    // returned the member to 사역 (#162). Navigation Compose keeps saveable
    // state per back stack entry, so the saved tab wins over the argument —
    // which stays the fallback for a fresh entry from elsewhere.
    var tab by rememberSaveable { mutableIntStateOf(participationTabIndex(initialTab)) }
    // The scroll position deserves the same treatment; returning to the top of
    // a long list is the same class of lost context.
    val nurtureListState = rememberLazyListState()
    val ministryListState = rememberLazyListState()
    val ministries = (state as? MinistryListUiState.Success)?.ministries.orEmpty()
    // No count while the list is loading or unavailable: "0" would claim there is nothing.
    val nurtureCount = nurture.trainings.size.takeIf { !nurture.isLoading && !nurture.isUnavailable }

    DnBackground(glows = DnGlows.action()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(
                title = if (tab == 0) "양육" else "사역",
                onBack = onBackClick,
            )

            Spacer(Modifier.height(14.dp))

            DnSegmented(
                options = listOf("양육", "사역"),
                selectedIndex = tab,
                onSelect = { tab = it },
                counts = listOf(nurtureCount?.toString(), ministries.size.toString()),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(18.dp))

            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    if (tab == 0) "함께 자랄 과정" else "함께 섬길 자리",
                    style = DnTheme.typography.titleLg,
                    color = c.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (tab == 0) "말씀 안에서 한 걸음 더 나아가는 과정들입니다."
                    else "은사를 나누며 교회를 함께 세워가는 팀들입니다.",
                    style = DnTheme.typography.caption,
                    color = c.textSecondary,
                )
            }

            Spacer(Modifier.height(18.dp))

            if (tab == TAB_SERVE_INDEX) {
                when (val s = state) {
                    is MinistryListUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = c.lime)
                    }

                    is MinistryListUiState.Error ->
                        DnErrorState(onRetry = viewModel::loadMinistries)

                    is MinistryListUiState.Success -> LazyColumn(
                        state = ministryListState,
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 60.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(s.ministries, key = { it.publicId }) { m ->
                            ParticipationRow(
                                icon = DnIcons.Users,
                                container = c.limeDim,
                                ink = c.limeInk,
                                name = m.name,
                                description = m.shortDescription,
                                metaIcon = DnIcons.User,
                                meta = m.leaderName?.let { "$it 리더" } ?: "리더 미정",
                                badge = null,
                                onClick = { onMinistryClick(m.publicId) },
                            )
                        }
                    }
                }
            } else {
                NurtureList(
                    state = nurture,
                    listState = nurtureListState,
                    onRetry = trainingViewModel::load,
                    onNurtureClick = onNurtureClick,
                )
            }
        }

        DnScrollEdge()
    }
}

/** The 양육 tab. Figma: section `22 · 양육 신청 · Zustände`, board `양육 리스트`. */
@Composable
private fun NurtureList(
    state: TrainingListUiState,
    listState: LazyListState,
    onRetry: () -> Unit,
    onNurtureClick: (String) -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val edge = Modifier.padding(horizontal = 20.dp)

    when {
        state.isLoading -> NurtureListSkeleton(edge)
        state.isUnavailable -> NurtureUnavailableCard(edge)
        state.hasError -> DnErrorState(onRetry = onRetry)
        state.trainings.isEmpty() -> NurtureMessageCard(DnIcons.Book, strings.nurtureEmpty, edge)
        else -> LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.trainings, key = { it.publicId }) { training: Training ->
                ParticipationRow(
                    icon = DnIcons.Book,
                    container = c.blueDim,
                    ink = c.blue,
                    name = training.name,
                    description = training.description,
                    metaIcon = if (training.window.isAlwaysOpen) DnIcons.Clock else DnIcons.Calendar,
                    meta = TrainingFormat.window(strings, training.window),
                    badge = { NurtureOpenBadge(open = training.openForRegistration) },
                    onClick = { onNurtureClick(training.publicId) },
                )
            }
        }
    }
}

/**
 * Which tab a route argument selects, and the fallback for anything else.
 *
 * A function rather than a comparison buried in the composable, so the
 * fallback the issue asks for is stated once and can be tested. It reads
 * `ParticipationRoute`'s own constants — this file used to keep a second
 * `TAB_SERVE` with the same value, which would have sent every entry to 양육
 * the day one of the two changed, without a compile error.
 */
fun participationTabIndex(routeTab: String): Int =
    if (routeTab == ParticipationRoute.TAB_SERVE) TAB_SERVE_INDEX else TAB_NURTURE_INDEX

const val TAB_NURTURE_INDEX = 0
const val TAB_SERVE_INDEX = 1

@Composable
private fun ParticipationRow(
    icon: ImageVector,
    container: Color,
    ink: Color,
    name: String,
    description: String?,
    metaIcon: ImageVector,
    meta: String?,
    badge: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    val c = DnTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(DnCardShape)
            .background(c.surface, DnCardShape)
            .border(1.dp, c.strokeSubtle, DnCardShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(22.dp))
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    name,
                    style = DnTheme.typography.captionStrong,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                badge?.invoke()
            }
            description?.let {
                Text(
                    it,
                    style = DnTheme.typography.caption,
                    color = c.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            meta?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(metaIcon, null, tint = c.textTertiary, modifier = Modifier.size(12.dp))
                    Text(it, style = DnTheme.typography.label, color = c.textTertiary)
                }
            }
        }

        Icon(DnIcons.ChevronRight, null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
    }
}
