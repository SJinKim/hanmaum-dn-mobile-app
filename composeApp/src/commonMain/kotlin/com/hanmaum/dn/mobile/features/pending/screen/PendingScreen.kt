package com.hanmaum.dn.mobile.features.pending.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.features.pending.presentation.PendingViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PendingScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val viewModel: PendingViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation Logic
    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let { route ->
            when (route) {
                NavRoute.Home -> onNavigateToHome()
                NavRoute.Login -> onNavigateToLogin()
                else -> { println("Navigation zu $route auf PendingScreen ignoriert.") }
            }
            viewModel.onNavigationHandled()
        }
    }

    // Snackbar for Meldungen
    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onNavigationHandled()
        }
    }

    Scaffold (
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon (z.B. Schloss oder Sanduhr)
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "가입 대기 중\n(Warte auf Freigabe)",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "가입 신청이 완료되었습니다.\n관리자의 승인 후 로그인이 가능합니다.\n잠시만 기다려 주세요.",
                // DE: Registrierung erfolgreich. Warte auf Admin-Bestätigung.
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                // STATUS PRÜFEN BUTTON
                Button(
                    onClick  = { viewModel.onCheckStatusClicked() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = MaterialTheme.shapes.extraSmall,
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("승인 상태 확인", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // LOGOUT BUTTON
                TextButton(onClick = { viewModel.onLogoutClicked() }) {
                    Text("로그아웃", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}