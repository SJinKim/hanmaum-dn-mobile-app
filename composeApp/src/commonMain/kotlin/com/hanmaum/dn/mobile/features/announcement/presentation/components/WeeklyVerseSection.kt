package com.hanmaum.dn.mobile.features.announcement.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanmaum.dn.mobile.core.presentation.theme.DeepCharcoal
import com.hanmaum.dn.mobile.core.presentation.theme.GoldLight
import com.hanmaum.dn.mobile.core.presentation.theme.MutedGray
import com.hanmaum.dn.mobile.core.presentation.theme.SanctuaryWhite

// TODO(api): Replace hardcoded strings with data from /api/v1/verses/weekly
// wired through VerseRepository → HomeViewModel in a future session.
@Composable
fun WeeklyVerseSection() {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DeepCharcoal)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "주간 암송 구절",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.05.sp,
            ),
            color = GoldLight,
        )

        Text(
            text = "여호와는 나의 목자시니 내게 부족함이 없으리로다",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = SanctuaryWhite,
        )

        Text(
            text = "시편 23:1",
            style = MaterialTheme.typography.labelMedium,
            color = MutedGray,
        )

        TextButton(
            onClick = { /* TODO: 암송 기능 추가 예정 */ },
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                text = "외우기 시작 →",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = GoldLight,
            )
        }
    }
}
