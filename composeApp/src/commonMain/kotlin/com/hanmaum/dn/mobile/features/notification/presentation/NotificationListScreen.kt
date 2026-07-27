package com.hanmaum.dn.mobile.features.notification.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.theme.AppMotion
import com.hanmaum.dn.mobile.features.notification.domain.model.Notification
import com.hanmaum.dn.mobile.features.notification.presentation.components.NotificationRow
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationListScreen(
    onBack: () -> Unit,
    onOpenAnnouncement: (String) -> Unit,
    viewModel: NotificationListViewModel = koinViewModel(),
) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(Unit) { viewModel.openAnnouncement.collect { id -> onOpenAnnouncement(id) } }

    // Infinite scroll: request the next page when the last item becomes visible.
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            state.hasNext && last >= state.items.lastIndex
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = strings.back,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = strings.notificationsTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Box {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { menuOpen = true },
                    enabled = state.items.isNotEmpty(),
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = strings.moreOptions,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(strings.notificationsReadAll) },
                        enabled = !state.allRead,
                        onClick = {
                            menuOpen = false
                            viewModel.onReadAll()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(strings.notificationsDeleteAll, color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            viewModel.deleteAll()
                        },
                    )
                }
            }
        }

        val error = state.error
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::load) { Text(strings.retry) }
                }
            }
            state.items.isEmpty() -> EmptyNotifications()
            else -> {
                val dayGroups = remember(state.items) { groupByDay(state.items) }
                val globalIndex = remember(state.items) {
                    state.items.withIndex().associate { (index, notification) -> notification.publicId to index }
                }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    dayGroups.forEach { (header, group) ->
                        item(key = "header-$header") {
                            Text(
                                text = when (header) {
                                    DayBucket.TODAY -> strings.notificationsToday
                                    DayBucket.YESTERDAY -> strings.notificationsYesterday
                                    DayBucket.EARLIER -> strings.notificationsEarlier
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                            )
                        }
                        items(group, key = { it.publicId }) { notification ->
                            StaggeredNotificationRow(
                                notification = notification,
                                index = globalIndex[notification.publicId] ?: 0,
                                timeText = relativeTime(notification.createdAt),
                                onClick = { viewModel.onItemClick(notification) },
                                onDelete = { viewModel.delete(notification) },
                            )
                        }
                    }
                    if (state.isLoadingMore) {
                        item(key = "loading-more") {
                            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// List entry stagger (DESIGN.md §8): fade + 8dp Y slide, 40ms delay per item, capped at 5.
@Composable
private fun StaggeredNotificationRow(
    notification: Notification,
    index: Int,
    timeText: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    var appeared by remember(notification.publicId) { mutableStateOf(false) }
    LaunchedEffect(notification.publicId) {
        delay(AppMotion.STAGGER_DELAY_MS.toLong() * index.coerceAtMost(5))
        appeared = true
    }
    val slidePx = with(LocalDensity.current) { 8.dp.toPx() }
    val alpha by animateFloatAsState(if (appeared) 1f else 0f, tween(200), label = "stagger-a")
    val offsetY by animateFloatAsState(if (appeared) 0f else slidePx, AppMotion.listItem, label = "stagger-y")

    // Swipe end-to-start to delete (immediate, no undo). The removal is driven by the
    // ViewModel dropping the item, so the row leaves composition after confirm.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = strings.notificationsDelete,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        NotificationRow(
            notification = notification,
            timeText = timeText,
            onClick = onClick,
        )
    }
}

@Composable
private fun EmptyNotifications() {
    val strings = LocalStrings.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = strings.notificationsEmpty,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal enum class DayBucket { TODAY, YESTERDAY, EARLIER }

internal fun groupByDay(
    items: List<Notification>,
    now: Instant = Clock.System.now(),
): List<Pair<DayBucket, List<Notification>>> {
    val zone = TimeZone.currentSystemDefault()
    val today = now.toLocalDateTime(zone).date
    return items
        .groupBy { n ->
            val date = n.createdAt.toLocalDateTime(zone).date
            when {
                date == today -> DayBucket.TODAY
                date == today.minus(DatePeriod(days = 1)) -> DayBucket.YESTERDAY
                else -> DayBucket.EARLIER
            }
        }
        .toList()
        .sortedBy { it.first.ordinal }
}

@Composable
private fun relativeTime(createdAt: Instant): String {
    val strings = LocalStrings.current
    val minutes = (Clock.System.now() - createdAt).inWholeMinutes
    return when {
        minutes < 1 -> strings.notificationTimeJustNow
        minutes < 60 -> strings.notificationTimeMinutesAgo(minutes.toInt())
        minutes < 60 * 24 -> strings.notificationTimeHoursAgo((minutes / 60).toInt())
        else -> strings.notificationTimeDaysAgo((minutes / (60 * 24)).toInt())
    }
}
