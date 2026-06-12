package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import hanmaumdnapp.composeapp.generated.resources.Res
import hanmaumdnapp.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

/** Full-screen lock overlay shown while the biometric app lock is engaged. */
@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    onUsePassword: () -> Unit,
) {
    val strings = LocalStrings.current
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier.height(96.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = strings.lockTitle,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.lockSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Text(strings.lockUnlockButton, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onUsePassword) {
                Text(strings.lockUsePassword, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
