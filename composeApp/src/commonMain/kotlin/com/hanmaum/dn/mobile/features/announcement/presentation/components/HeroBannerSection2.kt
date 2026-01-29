package com.hanmaum.dn.mobile.features.announcement.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroBannerSection2(
    banners: List<Announcement>,
    onBannerClick: (String) -> Unit
) {
    if (banners.isEmpty()) return

    // Wir starten in der "Mitte" einer riesigen Zahl, damit man
    // in beide Richtungen unendlich scrollen kann (optional, aber nice)
    val initialPage = (Int.MAX_VALUE / 2 / banners.size) * banners.size
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { Int.MAX_VALUE } // Unendliches Scrollen simulieren
    )

    // AUTO-SCROLL LOGIK (bleibt gleich)
    if (banners.size > 1) {
        LaunchedEffect(pagerState) {
            while (true) {
                delay(4000)
                try {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // WICHTIG: contentPadding bestimmt, wie viel man von den Nachbarn sieht.
        // Experimentiere mit diesem Wert! (z.B. 32.dp oder 64.dp)
        val padding = 48.dp

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = padding),
            pageSpacing = 0.dp, // Spacing machen wir über graphicsLayer
            modifier = Modifier.fillMaxWidth()
        ) { pageIndex ->
            // Wir holen das echte Item mittels Modulo, da wir unendlich viele Seiten haben
            val item = banners[pageIndex % banners.size]
            val categoryColor = Color(item.getAnnouncementCategoryColor())

            // Berechne, wie weit diese Seite von der Mitte entfernt ist.
            // 0.0 = exakt in der Mitte
            // 1.0 = eine volle Seitenbreite entfernt
            val pageOffset = (
                    (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                    ).absoluteValue

            // Konfiguration für den Effekt (Hieran kannst du drehen!)
            val minScale = 0.85f    // Wie klein sollen die Nachbarn werden? (85%)
            val minAlpha = 0.5f     // Wie durchsichtig sollen sie werden? (50%)

            Card(
                shape = RoundedCornerShape(24.dp), // Etwas rundere Ecken für modernen Look
                elevation = CardDefaults.cardElevation(
                    // Das mittlere Element bekommt mehr Schatten als die äußeren
                    defaultElevation = lerp(2.dp, 10.dp, 1f - pageOffset.coerceIn(0f, 1f))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp) // Etwas höher für mehr Impact
                    .graphicsLayer {
                        // Skalierung berechnen: Von 1.0 (Mitte) runter auf minScale (Außen)
                        val scale = lerp(1f, minScale, pageOffset.coerceIn(0f, 1f))
                        scaleX = scale
                        scaleY = scale

                        // Transparenz berechnen (optional)
                        alpha = lerp(1f, minAlpha, pageOffset.coerceIn(0f, 1f))
                    }
                    .clickable { onBannerClick(item.id.toString()) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(categoryColor)
                        .padding(24.dp) // Mehr Padding innen
                ) {
                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        // Tag
                        ContainerTag(label = item.getAnnouncementCategoryName())

                        Spacer(modifier = Modifier.height(12.dp))

                        // Titel
                        Text(
                            text = item.title,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold // Fetter für mehr Impact
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Indikatoren (angepasst für unendliches Scrollen)
        if (banners.size > 1) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // Wir nehmen den "echten" Index per Modulo
                val realCurrentPage = pagerState.currentPage % banners.size
                repeat(banners.size) { iteration ->
                    // Animierte Größe und Farbe für den Indikator
                    val isSelected = realCurrentPage == iteration
                    val width = if (isSelected) 24.dp else 8.dp
                    val color = if (isSelected) categoryColor4Indicator(banners[iteration].category) else Color.LightGray.copy(alpha = 0.5f)

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp)) // Ovale Form bei Auswahl
                            .background(color)
                            .height(8.dp)
                            .width(width)
                        // .animateContentSize() könnte man hier noch für smoothe Übergänge nutzen
                    )
                }
            }
        }
    }
}

// Kleine Hilfskomponente für einen schickeren Tag-Look
@Composable
fun ContainerTag(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

// Hilfsfunktion um die Indikator-Farbe passend zum aktiven Banner zu machen (optional)
private fun categoryColor4Indicator(category: String): Color {
    return when(category) {
        "MINISTRY" -> Color(0xFFE65100)
        "NOTICE"   -> Color(0xFFC62828)
        "EVENT"    -> Color(0xFF1565C0)
        else       -> Color.DarkGray
    }
}