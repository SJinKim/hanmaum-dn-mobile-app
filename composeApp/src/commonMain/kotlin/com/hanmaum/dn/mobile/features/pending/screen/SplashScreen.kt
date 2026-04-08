package com.hanmaum.dn.mobile.features.pending.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.features.pending.presentation.SplashViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = koinViewModel(),
    onNavigate: (NavRoute) -> Unit,
) {
    val destination by viewModel.navigateTo.collectAsState()

    LaunchedEffect(destination) {
        destination?.let { route ->
            onNavigate(route)
            viewModel.onNavigationHandled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "The Sanctuary",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "한마음 D+N",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                modifier    = Modifier.size(32.dp),
                color       = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
        }
    }
}
