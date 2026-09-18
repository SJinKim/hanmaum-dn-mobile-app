package com.hanmaum.dn.mobile.features.training.presentation.myapplications

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnScrollEdge
import com.hanmaum.dn.mobile.core.presentation.components.DnSegmented
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnPillShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.training.domain.model.MyApplication
import com.hanmaum.dn.mobile.features.training.presentation.TrainingFormat
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureCancelDialog
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureListSkeleton
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureMessageCard
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureStatusBadge
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureUnavailableCard
import org.koin.compose.viewmodel.koinViewModel

/**
 * 나의 신청 확인하기 — what the member has applied for, and how it stands.
 *
 * Figma: section `22 · 양육 신청 · Zustände`, board `나의 신청 · Zustände`.
 *
 * 양육 and 사역 sit under one segmented toggle, the same shape [ParticipationScreen] uses for
 * the two things one can apply for. Only 양육 has a server behind it: 사역 self-registration
 * does not exist yet (hanmaum-dn-server#170), so that half says 준비중입니다. and asks nobody.
 * The board draws a real 사역 list — deliberately not built until #170 decides what a 사역
 * application even is.
 */
@Composable
fun MyApplicationsScreen(
    onBackClick: () -> Unit,
    onApplicationClick: (String) -> Unit,
    viewModel: MyApplicationsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val listState = rememberLazyListState()
    // Saveable for the same reason ParticipationScreen's is: opening a detail takes this
    // screen out of composition, and a plain remember would drop the member back on 양육.
    var tab by rememberSaveable { mutableIntStateOf(TAB_NURTURE) }
    val edge = Modifier.padding(horizontal = 20.dp)

    DnBackground(glows = DnGlows.action()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = strings.myApplicationsTitle, onBack = onBackClick)

            Spacer(Modifier.height(14.dp))

            DnSegmented(
                options = listOf("양육", "사역"),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(18.dp))

            if (tab == TAB_SERVE) {
                NurtureMessageCard(DnIcons.Hourglass, strings.myApplicationsServeComingSoon, edge)
            } else {
                when {
                    state.isLoading -> NurtureListSkeleton(edge)
                    state.isUnavailable -> NurtureUnavailableCard(edge)
                    state.hasError -> NurtureMessageCard(
                        icon = DnIcons.AlertTriangle,
                        message = strings.myApplicationsError,
                        modifier = edge,
                        action = { RetryPill(onClick = viewModel::load) },
                    )

                    state.applications.isEmpty() ->
                        NurtureMessageCard(DnIcons.Book, strings.myApplicationsEmpty, edge)

                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 60.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            state.applications,
                            key = { "${it.trainingPublicId}-${it.externalCourseId}-${it.appliedAt}" },
                        ) { application ->
                            MyApplicationRow(
                                application = application,
                                onClick = { onApplicationClick(application.trainingPublicId) },
                                onCancel = { viewModel.openCancelDialog(application) },
                            )
                        }
                    }
                }
            }
        }

        DnScrollEdge()
    }

    state.cancelPrompt?.let { prompt ->
        NurtureCancelDialog(
            prompt = prompt,
            onConfirm = viewModel::confirmCancel,
            onDismiss = viewModel::dismissCancelDialog,
        )
    }
}

/**
 * One application: training, chosen course, 신청일 and the status tag.
 *
 * 신청 취소 sits in the row rather than behind the detail page, so the member can act where
 * they are looking — but only while the application is still running; 수료 and 취소됨 are
 * history and get no action (#245).
 */
@Composable
private fun MyApplicationRow(
    application: MyApplication,
    onClick: () -> Unit,
    onCancel: () -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current

    Column(
        Modifier
            .fillMaxWidth()
            .clip(DnCardShape)
            .background(c.surface, DnCardShape)
            .border(1.dp, c.strokeSubtle, DnCardShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(DnTileShape)
                    .background(c.blueDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DnIcons.Book, null, tint = c.blue, modifier = Modifier.size(20.dp))
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        application.trainingName,
                        style = DnTheme.typography.captionStrong,
                        color = c.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    NurtureStatusBadge(application.status)
                }

                application.courseName?.let {
                    Text(
                        it,
                        style = DnTheme.typography.caption,
                        color = c.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                application.appliedAt?.let { appliedAt ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(DnIcons.Calendar, null, tint = c.textTertiary, modifier = Modifier.size(12.dp))
                        Text(
                            "${strings.nurtureStatusAppliedOn} ${TrainingFormat.appliedOn(strings, appliedAt)}",
                            style = DnTheme.typography.label,
                            color = c.textTertiary,
                        )
                    }
                }
            }

            Icon(DnIcons.ChevronRight, null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
        }

        if (application.status.isActive) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(DnPillShape)
                    .border(1.dp, c.strokeStrong, DnPillShape)
                    .clickable(onClick = onCancel),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                Icon(DnIcons.X, null, tint = c.textSecondary, modifier = Modifier.size(13.dp))
                Text(strings.nurtureCancel, style = DnTheme.typography.label, color = c.textSecondary)
            }
        }
    }
}

/** 다시 시도 under the error message — outlined, because the message beside it carries the weight. */
@Composable
private fun RetryPill(onClick: () -> Unit) {
    val c = DnTheme.colors
    Row(
        Modifier
            .height(40.dp)
            .clip(DnPillShape)
            .border(1.dp, c.strokeStrong, DnPillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(LocalStrings.current.retry, style = DnTheme.typography.captionStrong, color = c.textPrimary)
    }
}

private const val TAB_NURTURE = 0
private const val TAB_SERVE = 1
