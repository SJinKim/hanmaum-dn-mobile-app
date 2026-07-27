package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.ErrorView
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.events.presentation.EventRsvpViewModel
import com.hanmaum.dn.mobile.features.events.presentation.components.EventRsvpSheet
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AnnouncementDetailScreen(
    announcementId: String,
    onBackClick: () -> Unit,
    onGoToList: () -> Unit,
) {
    val viewModel: AnnouncementDetailViewModel = koinViewModel(
        parameters = { parametersOf(announcementId) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Independent RSVP VM: this screen is not a top-level destination, so the global
    // EventRsvpHost is not mounted here. The sheet opens only on explicit CTA tap.
    val rsvpViewModel: EventRsvpViewModel = koinViewModel()
    val rsvpState by rsvpViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { rsvpViewModel.refresh() }
    val matching = rsvpState.events.firstOrNull { it.announcementId == announcementId }
    var sheetOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state.gone -> {
                AnnouncementUnavailableView(onBackClick = onBackClick, onGoToList = onGoToList)
            }
            state.hasError -> {
                // msg = null → ErrorView shows its localized generic message; retry can help here.
                ErrorView(msg = null, onRetry = { viewModel.loadAnnouncement() })
            }
            state.announcement != null -> {
                ArticleContent(
                    item = state.announcement!!,
                    onBackClick = onBackClick,
                    // EVENT announcement with a live, window-open RSVP → show the CTA.
                    showRsvpCta = state.announcement!!.category == "EVENT" && matching != null,
                    onRsvpClick = { sheetOpen = true },
                )
            }
        }

        if (sheetOpen && matching != null) {
            EventRsvpSheet(
                events = listOf(matching),
                checkingInId = rsvpState.checkingInId,
                checkedInIds = rsvpState.checkedInIds,
                rowErrors = rsvpState.rowErrors,
                onAttend = rsvpViewModel::checkIn,
                onDismiss = { sheetOpen = false },
            )
        }
    }
}

// Shown when a notification's announcement has left the active feed (expired/removed).
// A graceful terminal state — no doomed retry — that routes the user onward.
@Composable
private fun AnnouncementUnavailableView(
    onBackClick: () -> Unit,
    onGoToList: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = strings.back,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = strings.announcementUnavailableTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.announcementUnavailableBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onGoToList, shape = MaterialTheme.shapes.extraLarge) {
                Text(strings.announcementUnavailableAction)
            }
        }
    }
}

@Composable
private fun ArticleContent(
    item: Announcement,
    onBackClick: () -> Unit,
    showRsvpCta: Boolean = false,
    onRsvpClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Hero gradient area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer,
                        )
                    )
                ),
        ) {
            // Category badge — bottom-left
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
            ) {
                Text(
                    text     = item.getAnnouncementCategoryName().uppercase(),
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            // Nav icons — top overlay
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로",
                        tint               = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Row {
                    IconButton(onClick = { /* stub */ }) {
                        Icon(
                            imageVector        = Icons.Default.Share,
                            contentDescription = "공유",
                            tint               = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }

        // Article body
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))

            // Author row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier           = Modifier.size(40.dp),
                    tint               = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text  = "한마음 교회",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text  = item.startAt.take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Title
            Text(
                text  = item.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(20.dp))

            // Body intro (first paragraph — first 300 chars)
            val intro = item.body.take(300).let {
                if (item.body.length > 300) "$it..." else it
            }
            Text(
                text       = intro,
                style      = MaterialTheme.typography.bodyLarge,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
            )

            // Pull quote (if body is long enough)
            if (item.body.length > 60) {
                Spacer(Modifier.height(24.dp))
                PullQuote(text = item.body.take(120))
                Spacer(Modifier.height(24.dp))
            }

            // Remaining body
            if (item.body.length > 300) {
                Text(
                    text       = item.body.drop(300),
                    style      = MaterialTheme.typography.bodyLarge,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                )
            }

            Spacer(Modifier.height(32.dp))

            // Hashtags
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Hashtag(item.getAnnouncementCategoryName())
                Hashtag("한마음")
                Hashtag("소식")
            }

            if (showRsvpCta) {
                val strings = LocalStrings.current
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onRsvpClick,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(strings.rsvpAnnouncementCta) }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PullQuote(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.medium,
        color    = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text  = "\u201C",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text      = text,
                style     = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color     = MaterialTheme.colorScheme.onSurface,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
private fun Hashtag(tag: String) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
    ) {
        Text(
            text     = "#$tag",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
