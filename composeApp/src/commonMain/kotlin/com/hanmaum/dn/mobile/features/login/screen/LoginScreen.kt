package com.hanmaum.dn.mobile.features.login.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlow
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.security.BiometricResult
import com.hanmaum.dn.mobile.core.security.rememberBiometricAuthenticator
import com.hanmaum.dn.mobile.core.presentation.components.DnTintedButton
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTextField
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.login.presentation.LoginViewModel
import hanmaumdnapp.composeapp.generated.resources.Res
import hanmaumdnapp.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

/** Sign in. No back button — there is nothing behind this screen. */
@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToPending: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    val viewModel: LoginViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = DnTheme.colors

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val strings = LocalStrings.current
    val biometrics = rememberBiometricAuthenticator()
    // Armed once per screen: the settings and the stored credentials cannot
    // change while this screen is up, and re-reading them on every recomposition
    // would re-trigger the prompt.
    val autoLoginArmed = remember { viewModel.isAutoLoginArmed() && biometrics.isAvailable() }
    var promptRunning by remember { mutableStateOf(false) }

    suspend fun runBiometricPrompt() {
        if (promptRunning) return
        promptRunning = true
        val result = biometrics.authenticate(
            title = strings.biometricPromptTitle,
            subtitle = strings.biometricPromptSubtitle,
            cancelLabel = strings.biometricPromptCancel,
        )
        promptRunning = false
        // Cancelling is a choice, not a failure — fall back to the form quietly
        // and leave the button so it can be retried.
        if (result == BiometricResult.SUCCESS) viewModel.loginWithSavedCredentials()
    }

    // Offer the prompt as the screen opens, so the common case is one glance.
    LaunchedEffect(autoLoginArmed) {
        if (autoLoginArmed) runBiometricPrompt()
    }

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let { route ->
            when (route) {
                NavRoute.Home -> onNavigateToHome()
                NavRoute.PendingApproval -> onNavigateToPending()
                else -> Unit
            }
            viewModel.onNavigationHandled()
        }
    }

    DnBackground(glows = listOf(DnGlow(c.lime, 0.5f, 0.05f, 1.2f, 0.14f))) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))

            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "한마음 D+N",
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(c.textPrimary),
                modifier = Modifier.width(140.dp),
            )

            Spacer(Modifier.height(22.dp))
            Text(
                "다시 만나서 반가워요",
                style = DnTheme.typography.titleLg,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "한마음 D+N 계정으로 로그인하세요",
                style = DnTheme.typography.body,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(36.dp))

            DnTextField(
                label = "이메일",
                value = username,
                onValueChange = { username = it },
                placeholder = "hello@hanmaum.de",
                leading = DnIcons.Mail,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            DnTextField(
                label = "비밀번호",
                value = password,
                onValueChange = { password = it },
                placeholder = "••••••••",
                leading = DnIcons.Lock,
                trailing = DnIcons.Eye,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = DnTheme.typography.caption, color = c.red)
            }

            Spacer(Modifier.height(20.dp))
            DnPrimaryButton(
                label = if (state.isLoading) "로그인 중…" else "로그인",
                onClick = { viewModel.onLoginClicked(username, password) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            if (autoLoginArmed) {
                Spacer(Modifier.height(10.dp))
                val scope = rememberCoroutineScope()
                DnTintedButton(
                    label = strings.biometricSignIn,
                    onClick = { scope.launch { runBiometricPrompt() } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(26.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("아직 계정이 없으신가요?", style = DnTheme.typography.caption, color = c.textSecondary)
                Text(
                    "회원가입",
                    style = DnTheme.typography.captionStrong,
                    color = c.limeInk,
                    modifier = Modifier.clickable(onClick = onRegisterClick),
                )
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}
