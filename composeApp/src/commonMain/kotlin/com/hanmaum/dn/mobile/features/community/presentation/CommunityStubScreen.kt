package com.hanmaum.dn.mobile.features.community.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography

/**
 * 커뮤니티 — not built yet.
 *
 * An empty state that says what is coming and offers to notify, rather than
 * a bare "coming soon" that leaves people wondering whether it is broken.
 */
@Composable
fun CommunityStubScreen(onBackClick: () -> Unit) {
    val c = DnTheme.colors
    DnBackground(glows = DnGlows.information()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = "커뮤니티", onBack = onBackClick)

            Column(
                Modifier.fillMaxSize().padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(c.blueDim),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(DnIcons.Users, null, tint = c.blue, modifier = Modifier.size(44.dp))
                }

                Spacer(Modifier.height(18.dp))
                Text("곧 만나요", style = DnTheme.typography.titleLg, color = c.textPrimary)

                Spacer(Modifier.height(8.dp))
                Text(
                    "성도들이 서로 소식을 나누고 기도 제목을 올릴 수 있는 공간을 준비하고 있습니다.",
                    style = DnTheme.typography.body,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(c.surface2, RoundedCornerShape(percent = 50))
                        .border(1.dp, c.strokeSubtle, RoundedCornerShape(percent = 50))
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(DnIcons.Bell, null, tint = c.textPrimary, modifier = Modifier.size(17.dp))
                    Text("열리면 알려주세요", style = DnTheme.typography.captionStrong, color = c.textPrimary)
                }
            }
        }
    }
}
