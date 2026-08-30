package com.hanmaum.dn.mobile.features.attendance.presentation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.attendance.presentation.components.SlideToCheckIn
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import com.hanmaum.dn.mobile.features.events.presentation.EventRsvpViewModel

/**
 * 출석 체크.
 *
 * The check-in itself works off the existing definition endpoint. The
 * counters and the history below need a per-member attendance endpoint that
 * does not exist yet — see hanmaum-dn-server#114.
 */
@Composable
fun AttendanceScreen(
    onBackClick: () -> Unit,
    onRsvpClick: () -> Unit,
    viewModel: AttendanceViewModel = koinViewModel(),
    rsvpViewModel: EventRsvpViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val rsvpState by rsvpViewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { rsvpViewModel.refresh() }
    val c = DnTheme.colors

    DnBackground(glows = DnGlows.action()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = "출석 체크", onBack = onBackClick, onAction = { })

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(20.dp))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(DnCardShape)
                        .background(c.surface, DnCardShape)
                        .border(1.dp, c.strokeSubtle, DnCardShape)
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(c.limeDim),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(DnIcons.UserCheck, null, tint = c.limeInk, modifier = Modifier.size(34.dp))
                    }

                    Text(
                        state.definition?.title ?: "오늘은 예배가 없습니다",
                        style = DnTheme.typography.titleLg,
                        color = c.textPrimary,
                    )

                    state.definition?.let { def ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(DnIcons.Clock, null, tint = c.textTertiary, modifier = Modifier.size(14.dp))
                            Text(
                                "출석 시간 ${def.windowStart.take(5)} – ${def.windowEnd.take(5)}",
                                style = DnTheme.typography.caption,
                                color = c.textTertiary,
                            )
                        }
                    }

                    if (state.definition != null) {
                        SlideToCheckIn(
                            label = if (state.isCheckedIn) "출석 완료" else "밀어서 출석하기",
                            disabledLabel = "출석 시간이 아닙니다",
                            enabled = state.isInWindow && !state.isCheckedIn,
                            checkedIn = state.isCheckedIn,
                            isBusy = state.isCheckingIn,
                            onCheckIn = viewModel::checkIn,
                        )
                    }

                    state.checkInError?.let {
                        Text(it, style = DnTheme.typography.caption, color = c.red)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // The way back to an invitation that was put off. Hidden when
                // nothing is open, so the screen does not carry a dead row.
                if (rsvpState.pendingCount > 0) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(DnTileShape)
                            .background(c.surface, DnTileShape)
                            .border(1.dp, c.amber.copy(alpha = 0.45f), DnTileShape)
                            .clickable(onClick = onRsvpClick)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(13.dp)).background(c.amberDim),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(DnIcons.Calendar, null, tint = c.amber, modifier = Modifier.size(19.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "응답하지 않은 행사 ${rsvpState.pendingCount}건",
                                style = DnTheme.typography.captionStrong,
                                color = c.textPrimary,
                            )
                            Text(
                                "참석 여부를 알려주세요",
                                style = DnTheme.typography.caption,
                                color = c.textTertiary,
                            )
                        }
                        Icon(DnIcons.ChevronRight, null, tint = c.textTertiary, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.height(20.dp))
                }

                // TODO(hanmaum-dn-server#114): no per-member attendance summary
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    listOf("이번 달" to c.limeInk, "올해" to c.blue, "출석률" to c.amber)
                        .forEach { (label, accent) ->
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(DnTileShape)
                                    .background(c.surface, DnTileShape)
                                    .border(1.dp, c.strokeSubtle, DnTileShape)
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text("–", style = DnTheme.typography.stat, color = accent)
                                Text(label, style = DnTheme.typography.label, color = c.textTertiary)
                            }
                        }
                }

                Spacer(Modifier.height(22.dp))
                Text("최근 출석", style = DnTheme.typography.headline, color = c.textPrimary)
                Spacer(Modifier.height(12.dp))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(DnTileShape)
                        .background(c.surface, DnTileShape)
                        .border(1.dp, c.strokeSubtle, DnTileShape)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(DnIcons.Clock, null, tint = c.textTertiary, modifier = Modifier.size(22.dp))
                    Text(
                        "출석 기록은 아직 제공되지 않습니다",
                        style = DnTheme.typography.captionStrong,
                        color = c.textSecondary,
                    )
                    Text(
                        "서버에 개인 출석 내역 엔드포인트가 준비되면 여기에 표시됩니다.",
                        style = DnTheme.typography.caption,
                        color = c.textTertiary,
                    )
                }

                Spacer(Modifier.height(60.dp))
            }
        }
    }
}
