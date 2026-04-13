package com.hanmaum.dn.mobile.features.announcement.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanmaum.dn.mobile.core.presentation.theme.CardWhite
import com.hanmaum.dn.mobile.core.presentation.theme.CoralDark
import com.hanmaum.dn.mobile.core.presentation.theme.MutedGray
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroBannerSection(
    banners: List<Announcement>,
    onBannerClick: (String) -> Unit,
) {
    if (banners.isEmpty()) {
        HeroBannerLoading()
        return
    }

    val items = banners.take(5)
    val initialPage = (Int.MAX_VALUE / 2 / items.size) * items.size
    val pagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }

    AutoScrollEffect(pagerState = pagerState, itemCount = items.size)

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
        ) { pageIndex ->
            val item = items[pageIndex % items.size]
            HeroBannerCard(
                announcement = item,
                onClick = { onBannerClick(item.id) },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        HeroBannerIndicators(
            totalCount = items.size,
            currentIndex = pagerState.currentPage % items.size,
        )
    }
}

@Composable
private fun HeroBannerCard(
    announcement: Announcement,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(CoralDark, Color(0xFF1A0A0A))
                )
            )
            .clickable(onClick = onClick)
            .padding(24.dp),
    ) {
        // Eyebrow
        Text(
            text = "DN App",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.TopStart),
        )

        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Sermon title
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).sp,
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Service pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "주일 예배  •  ${announcement.startAt.take(10)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
            }

            // CTA button
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardWhite,
                    contentColor   = CoralDark,
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "예배 공지 읽기",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun HeroBannerIndicators(
    totalCount: Int,
    currentIndex: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(totalCount) { i ->
            val isSelected = i == currentIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) CoralDark
                        else MutedGray.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

@Composable
private fun HeroBannerLoading() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(CoralDark, Color(0xFF1A0A0A))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AutoScrollEffect(pagerState: PagerState, itemCount: Int) {
    if (itemCount > 1) {
        LaunchedEffect(pagerState, itemCount) {
            while (true) {
                delay(4000)
                try {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                } catch (_: Exception) {}
            }
        }
    }
}
