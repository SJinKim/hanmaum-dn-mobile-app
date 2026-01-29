package com.hanmaum.dn.mobile.features.announcement.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement

@Composable
fun LatestNewsSection(
    newsList: List<Announcement>,
    onItemClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    // Wenn die Liste leer ist, zeigen wir gar nichts an (oder alternativ einen "Empty State")
    if (newsList.isEmpty()) return

    val topFiveNews = newsList.take(5)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        //  === HEADER ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "최신 소식",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            // "더 보기" Button (Klein und Grau)
            Text(
                text = "더 보기 >",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier
                    .clickable { onViewAllClick() } // Klickbar!
                    .padding(4.dp) // Hitbox vergrößern
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // === Karten ===
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                topFiveNews.forEachIndexed { index, news ->
                    // UI für ein einzelnes Listen-Element
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(news.id) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dynamische Farbe basierend auf Kategorie
                        val categoryColor = Color(news.getAnnouncementCategoryColor())

                        // Kategorie Tag ([공지])
                        Text(
                            text = "[${news.getAnnouncementCategoryName()}]",
                            color = categoryColor,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Titel der News
                        Text(
                            text = news.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f) // Nimmt den restlichen Platz ein
                        )
                    }

                    // Trennlinie (HorizontalDivider) - aber nicht nach dem allerletzten Item
                    if (index < topFiveNews.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFFF5F5F5)
                        )
                    }
                }
            }
        }
    }
}