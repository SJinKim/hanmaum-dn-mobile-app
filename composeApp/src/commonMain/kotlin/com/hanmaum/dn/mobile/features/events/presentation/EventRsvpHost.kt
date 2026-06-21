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

    if (state.visible) {
        EventRsvpSheet(
            events = state.events,
            checkingInId = state.checkingInId,
            checkedInIds = state.checkedInIds,
            rowErrors = state.rowErrors,
            onAttend = viewModel::checkIn,
            onDismiss = viewModel::dismissAll,
        )
    }
}
