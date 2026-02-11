package com.hanmaum.dn.mobile.features.announcement.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import kotlin.math.absoluteValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

// Private Hilfsklasse
private data class BannerItem(val title: String, val color: Color)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroBannerSection(
    banners: List<Announcement>,
    onBannerClick: (String) -> Unit
) {
    if (banners.isEmpty()) return

    // Setup für unendliches Scrollen
    val initialPage = (Int.MAX_VALUE / 2 / banners.size) * banners.size
    val pagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }

    // Auto-Scroll Effekt
    AutoScrollEffect(pagerState, banners.size)

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            pageSpacing = 0.dp,
        ) { pageIndex ->
            val item = banners[pageIndex % banners.size]

            // Berechnung der Animation
            val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).absoluteValue

            BannerCard(
                item = item,
                pageOffset = pageOffset,
                onClick = { onBannerClick(item.id) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ausgelagerte Indikatoren
        if (banners.size > 1) {
            BannerIndicators(
                totalCount = banners.size,
                currentIndex = pagerState.currentPage % banners.size,
                getColorForIndex = { idx -> Color(banners[idx].getAnnouncementCategoryColor()) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AutoScrollEffect(pagerState: PagerState, itemCount: Int) {
    // Nur scrollen, wenn wir mehr als 1 Banner haben
    if (itemCount > 1) {
        LaunchedEffect(pagerState, itemCount) {
            while (true) {
                delay(3000) // 3 Sekunden warten
                try {
                    // Zur nächsten Seite animieren
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                } catch (e: Exception) {
                    // Fehler abfangen (z.B. wenn der User gerade selbst wischt)
                }
            }
        }
    }
}

// 2. SUB-KOMPONENTE: Die eigentliche Karte (UI pur)
@Composable
private fun BannerCard(
    item: Announcement,
    pageOffset: Float,
    onClick: () -> Unit
) {
    val minScale = 0.85f
    val minAlpha = 0.5f

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = lerp(2.dp, 10.dp, 1f - pageOffset.coerceIn(0f, 1f))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .graphicsLayer {
                val scale = lerp(1f, minScale, pageOffset.coerceIn(0f, 1f))
                scaleX = scale
                scaleY = scale
                alpha = lerp(1f, minAlpha, pageOffset.coerceIn(0f, 1f))
            }
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(item.getAnnouncementCategoryColor()))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                ContainerTag(label = item.getAnnouncementCategoryName())
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// 3. SUB-KOMPONENTE: Die Punkte unten drunter
@Composable
private fun BannerIndicators(
    totalCount: Int,
    currentIndex: Int,
    getColorForIndex: (Int) -> Color
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(totalCount) { iteration ->
            val isSelected = currentIndex == iteration
            val width = if (isSelected) 24.dp else 8.dp
            val color = if (isSelected) getColorForIndex(iteration) else Color.LightGray.copy(alpha = 0.5f)

            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .height(8.dp)
                    .width(width)
            )
        }
    }
}