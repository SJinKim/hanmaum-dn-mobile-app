package com.hanmaum.dn.mobile.features.ministry.presentation.detail

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography

/**
 * 양육 detail.
 *
 * Everything here is placeholder content: `/api/v1/trainings` returns only
 * publicId, name and sortOrder today. hanmaum-dn-server#113 tracks the fields
 * this page needs — period, time, place, leader, capacity, deadline — plus a
 * self-registration endpoint so the button can do something.
 */
@Composable
fun NurtureDetailScreen(
    publicId: String,
    onBackClick: () -> Unit,
) {
    val c = DnTheme.colors

    DnBackground(glows = DnGlows.information()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = "양육", onBack = onBackClick)

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(18.dp))

                DetailHero(
                    icon = DnIcons.Book,
                    container = c.blueDim,
                    ink = c.blue,
                    eyebrow = "말씀 안에서 자라기",
                    name = publicId,
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam " +
                        "nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam.",
                    style = DnTheme.typography.body,
                    color = c.textSecondary,
                )

                Spacer(Modifier.height(20.dp))
                DetailFacts(
                    rows = listOf(
                        Triple(DnIcons.Calendar, "기간", "미정 · 자리표시자"),
                        Triple(DnIcons.Clock, "시간", "미정 · 자리표시자"),
                        Triple(DnIcons.MapPin, "장소", "미정 · 자리표시자"),
                        Triple(DnIcons.User, "인도", "미정 · 자리표시자"),
                    )
                )

                Spacer(Modifier.height(22.dp))
                Text("이런 분께 권합니다", style = DnTheme.typography.headline, color = c.textPrimary)
                Spacer(Modifier.height(12.dp))
                listOf(
                    "Lorem ipsum dolor sit amet",
                    "Consetetur sadipscing elitr",
                    "Sed diam nonumy eirmod tempor",
                ).forEach { line ->
                    Row(
                        Modifier.padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(c.limeDim),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(DnIcons.Check, null, tint = c.limeInk, modifier = Modifier.size(13.dp))
                        }
                        Text(line, style = DnTheme.typography.caption, color = c.textSecondary)
                    }
                }

                Spacer(Modifier.height(12.dp))
                DetailCapacity(taken = null, total = null, deadline = null)

                Spacer(Modifier.height(28.dp))
                DnPrimaryButton(
                    label = "신청하기",
                    leading = DnIcons.UserCheck,
                    onClick = { /* enabled once #113 ships a registration endpoint */ },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(60.dp))
            }
        }
    }
}
