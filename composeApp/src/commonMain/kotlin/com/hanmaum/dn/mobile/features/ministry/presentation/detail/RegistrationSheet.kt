package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.theme.DnInnerShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography

/**
 * Applying asks for one optional sentence about yourself. Kept in a sheet so
 * the detail page stays readable and the decision to apply is one tap away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationSheet(
    note: String,
    isLoading: Boolean,
    error: String?,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = DnTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("신청하기", style = DnTheme.typography.title, color = c.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "간단한 소개를 남기시면 담당자가 참고합니다. (선택)",
                style = DnTheme.typography.caption,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(16.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(DnInnerShape)
                    .background(c.surface2, DnInnerShape)
                    .border(1.dp, c.strokeStrong, DnInnerShape)
                    .padding(16.dp),
            ) {
                BasicTextField(
                    value = note,
                    onValueChange = { if (it.length <= 500) onNoteChange(it) },
                    textStyle = DnTheme.typography.body.copy(color = c.textPrimary),
                    cursorBrush = SolidColor(c.lime),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (note.isEmpty()) {
                    Text(
                        "예) 찬양팀에서 베이스를 맡고 싶습니다.",
                        style = DnTheme.typography.body,
                        color = c.textTertiary,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "${note.length}/500",
                style = DnTheme.typography.label,
                color = c.textTertiary,
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = DnTheme.typography.caption, color = c.red)
            }

            Spacer(Modifier.height(16.dp))
            DnPrimaryButton(
                label = if (isLoading) "신청 중…" else "신청하기",
                onClick = onConfirm,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
