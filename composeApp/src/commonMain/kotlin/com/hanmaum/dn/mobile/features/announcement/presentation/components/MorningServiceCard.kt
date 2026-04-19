package com.hanmaum.dn.mobile.features.announcement.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.presentation.AttendanceUiState

private val DAY_KOREAN = mapOf(
    "MONDAY"    to "월요일",
    "TUESDAY"   to "화요일",
    "WEDNESDAY" to "수요일",
    "THURSDAY"  to "목요일",
    "FRIDAY"    to "금요일",
    "SATURDAY"  to "토요일",
    "SUNDAY"    to "일요일",
)

/**
 * Renders nothing (early return) when [AttendanceUiState.definition] is null,
 * i.e. no service is scheduled for today.
 */
@Composable
fun MorningServiceCard(
    state: AttendanceUiState,
    onCheckIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val def = state.definition ?: return  // hide card when no service today

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape     = MaterialTheme.shapes.large,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Icon
            Surface(
                shape    = CircleShape,
                color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector        = Icons.Default.HowToReg,
                    contentDescription = "출석 체크 아이콘",
                    modifier           = Modifier.padding(14.dp),
                    tint               = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(4.dp))

            // Title
            Text(
                text  = def.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Subtitle: "일요일  ·  10:30"
            Text(
                text  = formatSubtitle(def),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 0.1.em,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            // Check-in button
            val buttonEnabled = state.isInWindow && !state.isCheckedIn && !state.isCheckingIn
            Button(
                onClick  = onCheckIn,
                enabled  = buttonEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = MaterialTheme.colorScheme.primary,
                    contentColor           = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                if (state.isCheckingIn) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text  = if (state.isCheckedIn) "출석 완료 ✓" else "출석하기",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            // Error message (network/server error)
            if (state.checkInError != null) {
                Text(
                    text  = state.checkInError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (!state.isInWindow && !state.isCheckedIn) {
                Text(
                    text  = "출석 시간이 아닙니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatSubtitle(def: AttendanceDefinition): String {
    val day  = DAY_KOREAN[def.dayOfWeek] ?: def.dayOfWeek
    val time = def.windowStart.split(":").take(2).joinToString(":")
    return "$day  ·  $time"
}
