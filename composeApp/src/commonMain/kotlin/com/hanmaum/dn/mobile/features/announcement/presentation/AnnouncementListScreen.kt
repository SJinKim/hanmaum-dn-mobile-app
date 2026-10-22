package com.hanmaum.dn.mobile.features.announcement.presentation

import com.hanmaum.dn.mobile.core.i18n.displayDate
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.hanmaum.dn.mobile.core.presentation.components.DnErrorState
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnChip
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnImagePlaceholder
import com.hanmaum.dn.mobile.core.presentation.components.DnDock
import com.hanmaum.dn.mobile.core.presentation.components.DnScrollEdge
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import org.koin.compose.viewmodel.koinViewModel

/**
 * 소식 — the announcement list.
 *
 * Filtering happens on the category the model already carries, so the chips
 * need no extra endpoint. An announcement without an image keeps the
 * dashed placeholder.
 */
@Composable
fun AnnouncementListScreen(
    onBackClick: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    val strings = LocalStrings.current
    val viewModel: AnnouncementListViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = DnTheme.colors

    val categoryFilters = categoryFilters()
    var category by remember { mutableStateOf<String?>(null) }

    val filtered = remember(state.list, category) {
        category?.let { cat -> state.list.filter { it.category == cat } } ?: state.list
    }

    DnBackground(glows = DnGlows.information()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = strings.navNews, onBack = onBackClick)

            Spacer(Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    DnChip(strings.newsAll, selected = category == null, onClick = { category = null })
                }
                items(categoryFilters) { (code, label) ->
                    DnChip(label, selected = category == code, onClick = { category = code })
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = c.lime)
                }

                state.error != null ->
                    DnErrorState(onRetry = viewModel::loadAll)

                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(strings.newsEmpty, style = DnTheme.typography.body, color = c.textSecondary)
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = DnDock.contentInset()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.id }) { item ->
                        NewsRow(item = item, onClick = { onItemClick(item.id) })
                    }
                }
            }
        }

        DnScrollEdge()
    }
}

@Composable
private fun categoryFilters(): List<Pair<String, String>> {
    val strings = LocalStrings.current
    return listOf(
        "NOTICE" to strings.categoryNotice,
        "MINISTRY" to strings.ministryDetailTitle,
        "EVENT" to strings.categoryEvent,
    )
}

@Composable
private fun NewsRow(item: Announcement, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val c = DnTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DnCardShape)
            .background(c.surface, DnCardShape)
            .border(1.dp, c.strokeSubtle, DnCardShape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnnouncementImage(item.imageUrl, Modifier.size(92.dp), cornerRadius = 20.dp)

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategoryPill(item)
                    Text(
                        strings.displayDate(item.startAt),
                        style = DnTheme.typography.caption,
                        color = c.textTertiary,
                    )
                }
                Text(
                    item.title,
                    style = DnTheme.typography.headline,
                    color = c.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.body,
                    style = DnTheme.typography.caption,
                    color = c.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(c.surface3),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DnIcons.User, null, tint = c.textSecondary, modifier = Modifier.size(13.dp))
            }
            Text(strings.churchName, style = DnTheme.typography.caption, color = c.textSecondary)

            Spacer(Modifier.weight(1f))

            Row(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(c.surface2, RoundedCornerShape(percent = 50))
                    .border(1.dp, c.strokeStrong, RoundedCornerShape(percent = 50))
                    .clickable(onClick = onClick)
                    .padding(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(strings.newsReadFull, style = DnTheme.typography.captionStrong, color = c.textPrimary)
                Icon(DnIcons.ChevronRight, null, tint = c.textPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/**
 * The announcement's cover image, or the dashed placeholder when it has none —
 * shared by the list thumbnail and the home carousel.
 */
@Composable
internal fun AnnouncementImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    label: String? = null,
) {
    if (imageUrl == null) {
        DnImagePlaceholder(modifier, cornerRadius = cornerRadius, label = label)
        return
    }
    val shape = RoundedCornerShape(cornerRadius)
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(shape)
            .background(DnTheme.colors.surface2, shape),
    )
}
