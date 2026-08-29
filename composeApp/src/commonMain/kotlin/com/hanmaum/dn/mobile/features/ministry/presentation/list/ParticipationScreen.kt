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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.presentation.components.DnErrorState
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnScrollEdge
import com.hanmaum.dn.mobile.core.presentation.components.DnSegmented
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
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
    val c = DnTheme.colors

    var tab by remember { mutableIntStateOf(if (initialTab == TAB_SERVE) 1 else 0) }
    val ministries = (state as? MinistryListUiState.Success)?.ministries.orEmpty()

    DnBackground(glows = DnGlows.action()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(
                title = if (tab == 0) "양육" else "사역",
                onBack = onBackClick,
                onAction = { },
            )

            Spacer(Modifier.height(14.dp))

            DnSegmented(
                options = listOf("양육", "사역"),
                selectedIndex = tab,
                onSelect = { tab = it },
                counts = listOf(NURTURE_PLACEHOLDER.size.toString(), ministries.size.toString()),
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

            if (tab == 1) {
                when (val s = state) {
                    is MinistryListUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = c.lime)
                    }

                    is MinistryListUiState.Error ->
                        DnErrorState(onRetry = viewModel::loadMinistries)

                    is MinistryListUiState.Success -> LazyColumn(
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
                // TODO(hanmaum-dn-server#113): /api/v1/trainings returns only
                // publicId, name and sortOrder — not enough for this list.
                // Placeholder rows until the endpoint carries the course data.
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 60.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(NURTURE_PLACEHOLDER) { item ->
                        ParticipationRow(
                            icon = DnIcons.Book,
                            container = c.blueDim,
                            ink = c.blue,
                            name = item.first,
                            description = item.second,
                            metaIcon = DnIcons.Calendar,
                            meta = item.third,
                            badge = "자리표시자",
                            onClick = { onNurtureClick(item.first) },
                        )
                    }
                }
            }
        }

        DnScrollEdge()
    }
}

const val TAB_SERVE = "SERVE"

/** Stand-in content so the layout can be reviewed before #113 lands. */
private val NURTURE_PLACEHOLDER = listOf(
    Triple("Lorem ipsum", "Consetetur sadipscing elitr, sed diam nonumy eirmod.", "기간 미정"),
    Triple("Dolor sit amet", "Sed diam voluptua, at vero eos et accusam et justo.", "기간 미정"),
    Triple("Consetetur", "Stet clita kasd gubergren, no sea takimata sanctus.", "기간 미정"),
)

@Composable
private fun ParticipationRow(
    icon: ImageVector,
    container: Color,
    ink: Color,
    name: String,
    description: String,
    metaIcon: ImageVector,
    meta: String,
    badge: String?,
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
                )
                if (badge != null) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(c.surface2, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(badge, style = DnTheme.typography.label, color = c.textTertiary)
                    }
                }
            }
            Text(
                description,
                style = DnTheme.typography.caption,
                color = c.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(metaIcon, null, tint = c.textTertiary, modifier = Modifier.size(12.dp))
                Text(meta, style = DnTheme.typography.label, color = c.textTertiary)
            }
        }

        Icon(DnIcons.ChevronRight, null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
    }
}
