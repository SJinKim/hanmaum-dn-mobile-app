package com.hanmaum.dn.mobile.features.events.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp

/** Formats an ISO-8601 offset window into "MM.dd HH:mm – HH:mm" by string slicing (no TZ math). */
private fun formatWindow(start: String, end: String): String {
    fun date(s: String) = if (s.length >= 10) s.substring(5, 10).replace('-', '.') else s
    fun time(s: String) = if (s.length >= 16) s.substring(11, 16) else s
    return "${date(start)} ${time(start)} – ${time(end)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventRsvpSheet(
    events: List<EventRsvp>,
    checkingInId: String?,
    checkedInIds: Set<String>,
    rowErrors: Map<String, String>,
    onAttend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (events.isEmpty()) return
    val strings = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge, // rounded sheet (shape_large)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (events.size == 1) strings.rsvpSheetTitle else strings.rsvpMultiHeader,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))

            if (events.size == 1) {
                val e = events.first()
                SingleEvent(
                    event = e,
                    isCheckingIn = checkingInId == e.publicId,
                    isCheckedIn = e.publicId in checkedInIds,
                    error = rowErrors[e.publicId],
                    onAttend = { onAttend(e.publicId) },
                )
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    events.forEach { e ->
                        EventRow(
                            event = e,
                            isCheckingIn = checkingInId == e.publicId,
                            isCheckedIn = e.publicId in checkedInIds,
                            error = rowErrors[e.publicId],
                            onAttend = { onAttend(e.publicId) },
                        )
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text(strings.rsvpLater) }
        }
    }
}

@Composable
private fun SingleEvent(
    event: EventRsvp,
    isCheckingIn: Boolean,
    isCheckedIn: Boolean,
    error: String?,
    onAttend: () -> Unit,
) {
    val strings = LocalStrings.current
    Text(event.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    Text(
        text = formatWindow(event.windowStart, event.windowEnd),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onAttend,
        enabled = !isCheckingIn && !isCheckedIn,
        shape = MaterialTheme.shapes.extraLarge, // shape_full pill
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        when {
            isCheckingIn -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            isCheckedIn -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(strings.rsvpDone)
            }
            else -> Text(strings.rsvpAttend)
        }
    }
    ErrorLine(error)
}

@Composable
private fun EventRow(
    event: EventRsvp,
    isCheckingIn: Boolean,
    isCheckedIn: Boolean,
    error: String?,
    onAttend: () -> Unit,
) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium, // shape_medium card
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = formatWindow(event.windowStart, event.windowEnd),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ErrorLine(error)
            }
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = onAttend,
                enabled = !isCheckingIn && !isCheckedIn,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                when {
                    isCheckingIn -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    isCheckedIn -> Icon(Icons.Filled.Check, contentDescription = strings.rsvpDone, modifier = Modifier.size(18.dp))
                    else -> Text(strings.rsvpAttendShort)
                }
            }
        }
    }
}

@Composable
private fun ErrorLine(error: String?) {
    AnimatedVisibility(visible = error != null, enter = fadeIn(animationSpec = spring()), exit = fadeOut(animationSpec = spring())) {
        Box(modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
