package com.hanmaum.dn.mobile.features.bulletin.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.AppScreen
import com.hanmaum.dn.mobile.core.presentation.components.DnGlassIconButton
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.*
import com.hanmaum.dn.mobile.features.bulletin.domain.model.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BulletinScreen(onBack: () -> Unit, viewModel: BulletinViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BulletinContent(state, onBack, viewModel::refresh, viewModel::selectTab, viewModel::openHistory)
    if (state.historyOpen) {
        BulletinHistorySheet(state, viewModel::closeHistory, viewModel::selectEdition, viewModel::loadMoreHistory)
    }
}

@Composable
internal fun BulletinContent(
    state: BulletinUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onHistory: () -> Unit,
) {
    val strings = LocalStrings.current
    val b = strings.bulletin
    val c = DnTheme.colors
    AppScreen(
        title = b.title, onBack = onBack, compact = true,
        actions = { DnGlassIconButton(DnIcons.Calendar, b.history, onHistory) },
    ) { padding ->
        when {
            state.content != null -> {
                val read = state.content
                val edition = read.bulletin
                val blocks = remember(edition.sharingBlocks) { numberedSharingBlocks(edition.sharingBlocks) }
                val listState = rememberLazyListState()
                LaunchedEffect(state.selectedTab, state.selectedDate) { listState.scrollToItem(0) }
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = AppSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                ) {
                    if (read.cachedAt != null) item("cache") {
                        Column(Modifier.fillMaxWidth().background(c.amberDim, DnInnerShape).padding(AppSpacing.md)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                                Icon(DnIcons.Clock, null, tint = c.amber, modifier = Modifier.size(BulletinLayout.smallIcon))
                                Text(
                                    "${b.offline} ${read.cachedAt.toLocalDateTime(TimeZone.of("Europe/Berlin")).date}",
                                    style = DnTheme.typography.captionStrong, color = c.amber,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                )
                            }
                            TextButton(onClick = onRefresh, enabled = !state.isLoading) {
                                Text(if (state.isLoading) b.refreshing else strings.retry, color = c.amber)
                            }
                        }
                    }
                    item("cover") { BulletinCover(edition, showSermon = state.selectedTab == 0) }
                    if (state.selectedTab == 0) item("info") {
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                            InfoTile(b.scripture, edition.scriptureReference.orEmpty(), Modifier.weight(1f))
                            InfoTile(b.openingPrayer, edition.openingPrayerBy.orEmpty(), Modifier.weight(1f))
                            InfoTile(b.songs, edition.songs.size.toString(), Modifier.weight(1f))
                        }
                    }
                    item("tabs") { BulletinTabs(state.selectedTab, onSelectTab) }
                    if (state.selectedTab == 0) {
                        item("worship") {
                            Movement("01", edition.sectionTitle("SECTION_WORSHIP")) {
                                TimelineStep(b.openingSongs, last = edition.openingPrayerBy.isNullOrBlank()) {
                                    edition.songs.forEachIndexed { index, song ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                                            Text((index + 1).toString().padStart(2, '0'), color = c.limeInk, style = DnTheme.typography.captionStrong)
                                            Text(song, color = c.textPrimary, style = DnTheme.typography.bodyStrong, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                                edition.openingPrayerBy?.takeIf(String::isNotBlank)?.let {
                                    TimelineStep(b.openingPrayer, last = true) { EditorialText(it) }
                                }
                            }
                        }
                        item("offering") {
                            Movement("02", edition.sectionTitle("SECTION_OFFERING")) {
                                edition.offeringSongBy?.takeIf(String::isNotBlank)?.let {
                                    TimelineStep(b.offeringSong) { EditorialText(it) }
                                }
                                if (edition.announcements.isNotEmpty()) TimelineStep(b.announcements) {
                                    Column(Modifier.fillMaxWidth().background(c.surface, DnTileShape).padding(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                                        edition.announcements.forEachIndexed { index, news ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                                                Text((index + 1).toString(), style = DnTheme.typography.stat, color = c.blue)
                                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                                    EditorialText(news.title, Modifier.semantics { heading() })
                                                    news.body?.takeIf(String::isNotBlank)?.let { Text(it, style = DnTheme.typography.caption, color = c.textSecondary) }
                                                }
                                            }
                                        }
                                    }
                                }
                                edition.scriptureReference?.takeIf(String::isNotBlank)?.let {
                                    TimelineStep(b.scriptureReading) { EditorialText(it) }
                                }
                                TimelineStep(b.sermonProclamation, amber = true, last = edition.responsePrayerBy.isNullOrBlank()) {
                                    Column(
                                        Modifier.fillMaxWidth().background(c.amberDim, DnTileShape).bulletinClick { onSelectTab(1) }.padding(AppSpacing.md),
                                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                                    ) {
                                        Text(edition.sermonTitle, style = DnTheme.typography.title, color = c.textPrimary)
                                        Text(edition.sermonPreacher, style = DnTheme.typography.captionStrong, color = c.textSecondary)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(b.viewSharing, style = DnTheme.typography.captionStrong, color = c.amber)
                                            Icon(DnIcons.ChevronRight, null, tint = c.amber, modifier = Modifier.size(BulletinLayout.smallIcon))
                                        }
                                    }
                                }
                                edition.responsePrayerBy?.takeIf(String::isNotBlank)?.let {
                                    TimelineStep(b.responsePrayer, last = true) { EditorialText(it) }
                                }
                            }
                        }
                        item("sending") {
                            Movement("03", edition.sectionTitle("SECTION_SENDING")) {
                                edition.responseSong?.takeIf(String::isNotBlank)?.let {
                                    TimelineStep(b.responseSong) { EditorialText(it) }
                                }
                                val blessing = edition.sectionTitle("FIXED_BLESSING_PRAYER")
                                if (blessing.isNotBlank()) TimelineStep(b.closing, last = true) { EditorialText(blessing) }
                            }
                        }
                    } else {
                        item("sharingTitle") {
                            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                                Text(edition.sermonTitle, style = DnTheme.typography.titleLg, color = c.textPrimary, modifier = Modifier.semantics { heading() })
                                val questionCount = blocks.count { it.question != null }
                                Text(listOfNotNull(edition.scriptureReference, "$questionCount ${b.question}").joinToString(" · "),
                                    style = DnTheme.typography.caption, color = c.textSecondary)
                            }
                        }
                        if (blocks.isEmpty()) item("sharingEmpty") { Text(b.sharingEmpty, color = c.textSecondary, style = DnTheme.typography.body) }
                        items(blocks, key = { "block-${it.index}" }) { SharingBlock(it) }
                    }
                    item("footer") {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            edition.publishedAt?.let {
                                Text("${b.published} · ${it.toLocalDateTime(TimeZone.of("Europe/Berlin")).date}", style = DnTheme.typography.caption, color = c.textTertiary)
                            }
                            TextButton(onClick = onRefresh, enabled = !state.isLoading) { Text(if (state.isLoading) b.refreshing else b.refresh) }
                        }
                    }
                }
            }
            state.isLoading -> BulletinSkeleton(Modifier.padding(padding))
            else -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(AppSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Icon(if (state.failed) DnIcons.AlertTriangle else DnIcons.Book, null, tint = if (state.failed) c.red else c.textSecondary, modifier = Modifier.size(BulletinLayout.dateTile))
                Spacer(Modifier.height(AppSpacing.lg))
                Text(if (state.failed) b.errorTitle else if (state.selectedDate != null) b.unavailable else b.emptyTitle,
                    style = DnTheme.typography.title, color = c.textPrimary, textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Spacer(Modifier.height(AppSpacing.sm))
                Text(if (state.failed) b.errorBody else b.emptyBody, style = DnTheme.typography.body, color = c.textSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(AppSpacing.lg))
                DnPrimaryButton(if (state.failed) strings.retry else b.history, if (state.failed) onRefresh else onHistory)
            }
        }
    }
}

internal fun bulletinDate(date: LocalDate): String =
    "${date.monthNumber.toString().padStart(2, '0')}.${date.day.toString().padStart(2, '0')}"

@Composable
private fun BulletinCover(edition: Bulletin, showSermon: Boolean) {
    val b = LocalStrings.current.bulletin
    val c = DnTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("HANMAUM D+N · WEEKLY", style = DnTheme.typography.label, color = c.textTertiary)
            edition.volume?.let { Text("VOL. $it", style = DnTheme.typography.label, color = c.textTertiary) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(bulletinDate(edition.serviceDate), style = DnTheme.typography.display, color = c.textPrimary,
                modifier = Modifier.semantics { contentDescription = edition.serviceDate.toString() })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(b.sunday, style = DnTheme.typography.label, color = c.limeInk)
                Text(listOfNotNull(edition.serviceName, edition.serviceStartTime?.take(5)).joinToString(" · "), style = DnTheme.typography.caption, color = c.textSecondary)
            }
        }
        if (showSermon) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                Text(b.sermon, style = DnTheme.typography.label, color = c.amber)
                Text(edition.sermonTitle, style = DnTheme.typography.titleLg, color = c.textPrimary, modifier = Modifier.semantics { heading() })
                Text(listOfNotNull(edition.sermonPreacher, edition.scriptureReference).joinToString(" · "), style = DnTheme.typography.caption, color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(DnTheme.colors.surface, DnInnerShape).padding(AppSpacing.sm), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(label, style = DnTheme.typography.caption, color = DnTheme.colors.textTertiary)
        Text(value.ifBlank { "—" }, style = DnTheme.typography.bodyStrong, color = DnTheme.colors.textPrimary)
    }
}

@Composable
private fun BulletinTabs(selected: Int, onSelect: (Int) -> Unit) {
    val b = LocalStrings.current.bulletin
    val c = DnTheme.colors
    Row(Modifier.fillMaxWidth().background(c.surface2, DnPillShape).padding(AppSpacing.xs).selectableGroup()) {
        listOf(b.worshipOrder, b.sermonSharing).forEachIndexed { index, title ->
            Box(Modifier.weight(1f).heightIn(min = BulletinLayout.touchTarget).clip(DnPillShape)
                .background(if (index == selected) c.lime else Color.Transparent)
                .selectable(index == selected, role = Role.Tab, onClick = { onSelect(index) })
                .padding(AppSpacing.sm), contentAlignment = Alignment.Center) {
                Text(title, style = DnTheme.typography.captionStrong, color = if (index == selected) c.onLime else c.textSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Movement(number: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(number, style = DnTheme.typography.stat, color = DnTheme.colors.limeInk)
            Text(title, style = DnTheme.typography.headline, color = DnTheme.colors.textPrimary, modifier = Modifier.weight(1f).semantics { heading() })
        }
        Column(content = content)
    }
}

@Composable
private fun TimelineStep(label: String, last: Boolean = false, amber: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val c = DnTheme.colors
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Box(Modifier.width(BulletinLayout.rail).fillMaxHeight().drawBehind {
            val radius = BulletinLayout.node.toPx() / 2
            val top = radius + AppSpacing.xs.toPx()
            if (!last) drawLine(c.strokeStrong, Offset(size.width / 2, top + radius + AppSpacing.xs.toPx()), Offset(size.width / 2, size.height), BulletinLayout.line.toPx())
            drawCircle(if (amber) c.amber else c.strokeStrong, radius, Offset(size.width / 2, top))
        })
        Column(Modifier.weight(1f).padding(bottom = if (last) AppSpacing.xs else AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(label, style = DnTheme.typography.caption, color = c.textTertiary)
            content()
        }
    }
}

@Composable
private fun EditorialText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = DnTheme.typography.bodyStrong, color = DnTheme.colors.textPrimary)
}

internal data class NumberedSharingBlock(val index: Int, val block: BulletinSharingBlock, val question: Int?)
internal fun numberedSharingBlocks(blocks: List<BulletinSharingBlock>): List<NumberedSharingBlock> {
    var question = 0
    return blocks.mapIndexed { index, block -> NumberedSharingBlock(index, block, if (block.type == SharingBlockType.QUESTION) ++question else null) }
}

@Composable
private fun SharingBlock(item: NumberedSharingBlock) {
    val c = DnTheme.colors
    val block = item.block
    val questionLabel = LocalStrings.current.bulletin.question
    when (block.type) {
        SharingBlockType.HEADING -> Text(block.text, style = DnTheme.typography.headline, color = c.textPrimary, modifier = Modifier.semantics { heading() })
        SharingBlockType.PARAGRAPH -> Text(block.text, style = DnTheme.typography.body, color = c.textSecondary)
        SharingBlockType.SCRIPTURE -> Column(Modifier.fillMaxWidth().background(c.amberDim, DnTileShape).padding(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text("“", style = DnTheme.typography.display, color = c.amber)
            Text(block.text, style = DnTheme.typography.title, color = c.textPrimary)
            block.reference?.takeIf(String::isNotBlank)?.let { Text(it, style = DnTheme.typography.captionStrong, color = c.amber) }
        }
        SharingBlockType.QUESTION -> Column(Modifier.fillMaxWidth().background(c.surface, DnTileShape).padding(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text("Q${item.question}", style = DnTheme.typography.stat, color = c.limeInk,
                modifier = Modifier.semantics { contentDescription = "$questionLabel ${item.question}" })
            EditorialText(block.text)
        }
    }
}

@Composable
private fun BulletinSkeleton(modifier: Modifier) {
    val label = LocalStrings.current.bulletin.loading
    Column(modifier.fillMaxSize().padding(AppSpacing.md).semantics { contentDescription = label }, verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
        Box(Modifier.width(BulletinLayout.skeletonDateWidth).height(AppSpacing.sm).background(DnTheme.colors.surface3, DnInnerShape))
        Box(Modifier.width(BulletinLayout.skeletonDateWidth).height(BulletinLayout.skeletonDateHeight).background(DnTheme.colors.surface3, DnInnerShape))
        Box(Modifier.width(BulletinLayout.skeletonTitleWidth).height(AppSpacing.lg).background(DnTheme.colors.surface3, DnInnerShape))
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            repeat(3) { Box(Modifier.weight(1f).height(BulletinLayout.dateTile).background(DnTheme.colors.surface, DnInnerShape)) }
        }
        repeat(4) { Box(Modifier.fillMaxWidth().height(BulletinLayout.touchTarget).background(DnTheme.colors.surface2, DnInnerShape)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulletinHistorySheet(state: BulletinUiState, onDismiss: () -> Unit, onSelect: (LocalDate?) -> Unit, onLoadMore: () -> Unit) {
    val s = LocalStrings.current
    val b = s.bulletin
    val c = DnTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface, contentColor = c.textPrimary) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.7f), contentPadding = PaddingValues(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            item("title") { Text(b.history, style = DnTheme.typography.title, modifier = Modifier.semantics { heading() }) }
            item("current") { TextButton(onClick = { onSelect(null) }) { Text(b.current) } }
            items(state.history, key = BulletinSummary::publicId) { edition ->
                Column(Modifier.fillMaxWidth().heightIn(min = BulletinLayout.touchTarget).clip(DnInnerShape).background(c.surface2).bulletinClick { onSelect(edition.serviceDate) }.padding(AppSpacing.md)) {
                    Text(listOfNotNull(edition.serviceDate.toString(), edition.volume?.let { "VOL. $it" }).joinToString(" · "), style = DnTheme.typography.caption, color = c.textSecondary)
                    edition.sermonTitle?.let { Text(it, style = DnTheme.typography.bodyStrong) }
                }
            }
            item("status") {
                when {
                    state.historyLoading -> Text(b.loading, style = DnTheme.typography.body, color = c.textSecondary)
                    state.historyFailed -> Column {
                        Text(b.errorTitle, style = DnTheme.typography.body, color = c.red)
                        TextButton(onClick = onLoadMore) { Text(s.retry) }
                    }
                    state.historyHasNext -> TextButton(onClick = onLoadMore) { Text(b.loadMore) }
                    state.history.isEmpty() -> Text(b.historyEmpty, style = DnTheme.typography.body, color = c.textSecondary)
                }
            }
        }
    }
}

@Composable
fun BulletinHomeCard(read: BulletinRead, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val b = LocalStrings.current.bulletin
    val edition = read.bulletin
    val c = DnTheme.colors
    Row(modifier.fillMaxWidth().clip(DnTileShape).background(c.surface).bulletinClick(onClick).padding(AppSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.size(BulletinLayout.dateTile).background(c.limeDim, DnInnerShape), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(b.sunday, style = DnTheme.typography.label, color = c.limeInk)
            Text(bulletinDate(edition.serviceDate), style = DnTheme.typography.captionStrong, color = c.limeInk)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(if (read.cachedAt == null) b.current else "${b.offline} ${read.cachedAt.toLocalDateTime(TimeZone.of("Europe/Berlin")).date}", style = DnTheme.typography.label, color = c.limeInk)
            Text(edition.sermonTitle, style = DnTheme.typography.bodyStrong, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(edition.serviceName, edition.sermonPreacher).joinToString(" · "), style = DnTheme.typography.caption, color = c.textSecondary)
        }
        Icon(DnIcons.ChevronRight, null, tint = c.textTertiary, modifier = Modifier.size(BulletinLayout.smallIcon))
    }
}

@Composable
private fun Modifier.bulletinClick(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, AppMotion.screenPush)
    return scale(scale).clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
}
