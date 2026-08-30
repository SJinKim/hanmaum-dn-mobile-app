package com.hanmaum.dn.mobile.features.events.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hanmaum.dn.mobile.features.events.presentation.components.EventRsvpSheet
import org.koin.compose.viewmodel.koinViewModel

/**
 * Owns the shared RSVP ViewModel and refreshes on every app foreground (ON_START),
 * which also covers the first reach of Home. Renders the auto-prompt sheet.
 */
@Composable
fun EventRsvpHost(viewModel: EventRsvpViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The soonest unanswered event, and only that one. Stacking every open
    // invitation into one sheet turns a question into a wall; the rest wait on
    // the RSVP screen, which is exactly what "나중에" leads to.
    val next = state.pending.firstOrNull { it.myStatus == null }
    if (state.visible && next != null) {
        EventRsvpSheet(
            event = next,
            isResponding = state.respondingTo == next.publicId,
            errorMessage = state.rowErrors[next.publicId],
            onRespond = { status -> viewModel.respond(next.publicId, status) },
            onDismiss = viewModel::dismissSheet,
        )
    }
}
