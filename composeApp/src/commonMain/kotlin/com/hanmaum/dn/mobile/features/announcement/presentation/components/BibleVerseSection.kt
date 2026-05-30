package com.hanmaum.dn.mobile.features.announcement.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BibleVerseSection(onViewAllClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "오늘의 말씀",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.05.sp,
            ),
            color = MaterialTheme.colorScheme.outline,
        )

        Text(
            text = "\"빛이 어둠에 비치되 어둠이 깨닫지 못하더라\"",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = "요한복음 1:5",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )

        TextButton(
            onClick = onViewAllClick,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                text = "확인하기 →",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
