package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings

@Composable
fun ErrorView(
    msg: String?,
    onRetry: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(
        modifier            = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector        = Icons.Rounded.Warning,
            contentDescription = "Error",
            tint               = MaterialTheme.colorScheme.error,
            modifier           = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text  = strings.errorOccurred,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (msg != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text      = msg,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape   = MaterialTheme.shapes.extraSmall,
            colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            Text(strings.retry, style = MaterialTheme.typography.labelLarge)
        }
    }
}
