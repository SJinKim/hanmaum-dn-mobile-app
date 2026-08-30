package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.presentation.components.DnErrorState
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hanmaum.dn.mobile.features.events.presentation.EventRsvpViewModel
import com.hanmaum.dn.mobile.features.events.presentation.components.EventRsvpSheet

/**
 * 소식 detail.
 *
 * The hero image was dropped from the design — the article opens straight
 * into its content. The key-facts block shows the date the model carries;
 * place and deadline are placeholders until hanmaum-dn-server#112 lands.
 */
@Composable
fun AnnouncementDetailScreen(
    announcementId: String,
    onBackClick: () -> Unit,
) {
    val viewModel: AnnouncementDetailViewModel = koinViewModel(
        parameters = { parametersOf(announcementId) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Own RSVP ViewModel: this screen is not a top-level destination, so the
    // global EventRsvpHost is not mounted over it. The sheet opens only when
    // the member taps the CTA, never on its own.
    val rsvpViewModel: EventRsvpViewModel = koinViewModel()
    val rsvpState by rsvpViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { rsvpViewModel.refresh() }
    val matchingRsvp = rsvpState.events.firstOrNull { it.announcementId == announcementId }
    var rsvpSheetOpen by remember { mutableStateOf(false) }

    val c = DnTheme.colors

    DnBackground(glows = DnGlows.information()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = "소식", onBack = onBackClick, onAction = { })

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = c.lime)
                }

                // Reached from a notification whose announcement has since
                // expired or been removed. Retrying cannot bring it back, so
                // the only offer is a way onward.
                state.gone ->
                    DnErrorState(onGoHome = onBackClick)

                state.hasError ->
                    DnErrorState(onRetry = viewModel::loadAnnouncement)

                state.announcement != null -> {
                    val item = state.announcement!!
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                    ) {
                        Spacer(Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CategoryPill(item)
                            Text(
                                item.startAt.take(10),
                                style = DnTheme.typography.caption,
                                color = c.textTertiary,
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(item.title, style = DnTheme.typography.titleLg, color = c.textPrimary)

                        Spacer(Modifier.height(18.dp))
                        AuthorRow()

                        Spacer(Modifier.height(20.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(c.strokeSubtle)
                        )
                        Spacer(Modifier.height(20.dp))

                        Text(item.body, style = DnTheme.typography.body, color = c.textSecondary)

                        Spacer(Modifier.height(20.dp))
                        KeyFacts(
                            rows = listOf(
                                Triple(DnIcons.Calendar, "일시", formatRange(item.startAt, item.endAt)),
                                // TODO(hanmaum-dn-server#112): no location on AnnouncementDto
                                Triple(DnIcons.MapPin, "장소", "Lorem ipsum dolor"),
                                Triple(DnIcons.Clock, "문의", "교회 사무실"),
                            )
                        )

                        // Only for an EVENT announcement whose RSVP window is
                        // open right now — otherwise the button would promise
                        // something the server would refuse.
                        if (item.category == "EVENT" && matchingRsvp != null) {
                            Spacer(Modifier.height(20.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(DnCardShape)
                                    .background(c.limeDim, DnCardShape)
                                    .border(1.dp, c.lime.copy(alpha = 0.45f), DnCardShape)
                                    .clickable { rsvpSheetOpen = true }
                                    .padding(horizontal = 18.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(DnIcons.UserCheck, null, tint = c.limeInk, modifier = Modifier.size(20.dp))
                                Text("참석 여부 알리기", style = DnTheme.typography.bodyStrong, color = c.limeInk)
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("#${item.getAnnouncementCategoryName()}", "#한마음", "#소식").forEach { tag ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(c.surface2, RoundedCornerShape(percent = 50))
                                        .border(1.dp, c.strokeSubtle, RoundedCornerShape(percent = 50))
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                ) {
                                    Text(tag, style = DnTheme.typography.caption, color = c.textSecondary)
                                }
                            }
                        }

                        Spacer(Modifier.height(60.dp))
                    }
                }
            }
        }

        if (rsvpSheetOpen && matchingRsvp != null) {
            EventRsvpSheet(
                event = matchingRsvp,
                isResponding = rsvpState.respondingTo == matchingRsvp.publicId,
                errorMessage = rsvpState.rowErrors[matchingRsvp.publicId],
                onRespond = { status -> rsvpViewModel.respond(matchingRsvp.publicId, status) },
                onDismiss = { rsvpSheetOpen = false },
            )
        }
    }
}

@Composable
private fun AuthorRow() {
    val c = DnTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(c.surface2)
                .border(1.dp, c.strokeSubtle, RoundedCornerShape(percent = 50)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(DnIcons.User, null, tint = c.textSecondary, modifier = Modifier.size(19.dp))
        }
        Column {
            Text("한마음 교회", style = DnTheme.typography.captionStrong, color = c.textPrimary)
            Text("관리자", style = DnTheme.typography.caption, color = c.textTertiary)
        }
    }
}

/**
 * The block people look at first on an announcement: when, where, who to ask.
 * Blue because it informs rather than asks for an action.
 */
@Composable
internal fun KeyFacts(rows: List<Triple<ImageVector, String, String>>) {
    val c = DnTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DnCardShape)
            .background(c.blueDim, DnCardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { (icon, label, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.blue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = c.onBlue, modifier = Modifier.size(17.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(label, style = DnTheme.typography.label, color = c.textTertiary)
                    Text(value, style = DnTheme.typography.captionStrong, color = c.textPrimary)
                }
            }
        }
    }
}

private fun formatRange(startAt: String, endAt: String?): String {
    val start = startAt.take(10)
    val end = endAt?.take(10)
    return if (end == null || end == start) start else "$start – $end"
}
