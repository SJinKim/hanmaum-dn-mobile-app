package com.hanmaum.dn.mobile.features.pending.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlow
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.pending.presentation.PendingViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Waiting for the church office to approve the account. */
@Composable
fun PendingScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToRejected: () -> Unit,
) {
    val viewModel: PendingViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current
    val c = DnTheme.colors

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let { route ->
            when (route) {
                NavRoute.Home -> onNavigateToHome()
                NavRoute.Login -> onNavigateToLogin()
                NavRoute.Rejected -> onNavigateToRejected()
                else -> Unit
            }
            viewModel.onNavigationHandled()
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onNavigationHandled()
        }
    }

    DnBackground(glows = listOf(DnGlow(c.amber, 0.5f, 0.2f, 1.2f, 0.12f))) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 36.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(c.amberDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DnIcons.Hourglass, null, tint = c.amber, modifier = Modifier.size(46.dp))
            }

            Spacer(Modifier.height(20.dp))
            Text(
                strings.pendingTitle,
                style = DnTheme.typography.titleLg,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))
            Text(
                strings.pendingBody,
                style = DnTheme.typography.body,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            if (state.isLoading) {
                CircularProgressIndicator(color = c.lime)
            } else {
                DnPrimaryButton(
                    label = strings.checkStatus,
                    onClick = viewModel::onCheckStatusClicked,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(onClick = viewModel::onLogoutClicked)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(DnIcons.LogOut, null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        strings.profileLogout,
                        style = DnTheme.typography.captionStrong,
                        color = c.textSecondary,
                    )
                }
            }
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}
