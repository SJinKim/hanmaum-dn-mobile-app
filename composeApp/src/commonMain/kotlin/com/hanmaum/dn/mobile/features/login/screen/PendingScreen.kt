package com.hanmaum.dn.mobile.features.login.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PendingScreen(
    onCheckStatusClick: () -> Unit, // Button "Status prüfen"
    onLogoutClick: () -> Unit      // Button "Abmelden"
) {
    Scaffold { padding ->
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

            // STATUS PRÜFEN BUTTON
            Button(
                onClick = onCheckStatusClick,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("승인 상태 확인 (Status prüfen)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LOGOUT BUTTON
            TextButton(onClick = onLogoutClick) {
                Text("로그아웃 (Abmelden)")
            }
        }
    }
}