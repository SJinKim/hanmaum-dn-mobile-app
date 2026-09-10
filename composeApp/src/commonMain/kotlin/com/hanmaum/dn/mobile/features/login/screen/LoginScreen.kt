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
import com.hanmaum.dn.mobile.core.security.rememberBiometricVault
import com.hanmaum.dn.mobile.core.presentation.components.DnTintedButton
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTextField
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.navigation.LoginRoute
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
    /**
     * Code from [LoginRoute], set when the member arrives straight from
     * registering. Shown once, above the form, in the positive colour: their
     * account exists, so this is news rather than a failure.
     */
    notice: String? = null,
    onNavigateToHome: () -> Unit,
    onNavigateToPending: () -> Unit,
    onNavigateToRejected: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    val viewModel: LoginViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = DnTheme.colors

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val strings = LocalStrings.current
    val vault = rememberBiometricVault()
    // Read once per screen: neither the setting nor the sealed secret can change
    // while this screen is up.
    val faceIdArmed = remember { viewModel.canFaceIdSignIn(vault) && vault.isAvailable() }
    var promptRunning by remember { mutableStateOf(false) }

    // The vault raises the prompt and, on a real match, hands back the refresh
    // token; the ViewModel trades it for a session. Cancelling is a choice, not
    // a failure — the form is right there and the button stays for a retry.
    suspend fun runFaceIdSignIn() {
        if (promptRunning) return
        promptRunning = true
        viewModel.signInWithFaceId(
            vault = vault,
            title = strings.lockTitle,
            subtitle = strings.lockSubtitle,
            cancelLabel = strings.lockUsePassword,
        )
        promptRunning = false
    }

    // No prompt of its own accord. It used to be raised the moment this screen
    // entered composition — which happens while the splash is still on screen,
    // so Face ID appeared to come out of the waiting screen, unbidden, on every
    // launch (#212). The member starts it now, with the button below.

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let { route ->
            when (route) {
                NavRoute.Home -> onNavigateToHome()
                NavRoute.PendingApproval -> onNavigateToPending()
                NavRoute.Rejected -> onNavigateToRejected()
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

            // Nothing hides this card: no timer, no auto-dismiss. It stays
            // until the member acts, which is well past the four seconds it
            // takes to read — and a message telling someone to go and confirm
            // their email is the last thing that should time out from under
            // them. It sits above the form because it explains why the form is
            // there at all.
            notice?.let { code ->
                val body = when (code) {
                    LoginRoute.NOTICE_VERIFY_EMAIL -> strings.noticeVerifyEmailBody
                    LoginRoute.NOTICE_REGISTERED -> strings.noticeRegisteredBody
                    else -> null
                }
                body?.let {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(c.limeDim, RoundedCornerShape(20.dp))
                            .border(1.dp, c.lime, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(DnIcons.Mail, null, tint = c.limeInk, modifier = Modifier.size(18.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                strings.noticeRegisteredTitle,
                                style = DnTheme.typography.captionStrong,
                                color = c.limeInk,
                            )
                            Text(it, style = DnTheme.typography.caption, color = c.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

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

            if (faceIdArmed) {
                Spacer(Modifier.height(10.dp))
                val scope = rememberCoroutineScope()
                DnTintedButton(
                    label = strings.loginSignInWithFaceId,
                    onClick = { scope.launch { runFaceIdSignIn() } },
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
