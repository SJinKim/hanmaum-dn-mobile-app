package com.hanmaum.dn.mobile.features.login.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.navigation.LoginRoute
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlow
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTintedButton
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.*
import com.hanmaum.dn.mobile.core.security.rememberBiometricVault
import com.hanmaum.dn.mobile.core.security.rememberBrowserAuthentication
import com.hanmaum.dn.mobile.features.login.presentation.LoginViewModel
import hanmaumdnapp.composeapp.generated.resources.Res
import hanmaumdnapp.composeapp.generated.resources.logo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

/** No scaffold top bar: there is no destination behind this auth entry. */
@Composable
fun LoginScreen(
    notice: String? = null,
    onNavigateToHome: () -> Unit,
    onNavigateToPending: () -> Unit,
    onNavigateToRejected: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    val viewModel: LoginViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val browser = rememberBrowserAuthentication()
    val vault = rememberBiometricVault()
    val scope = rememberCoroutineScope()
    var promptRunning by remember { mutableStateOf(false) }
    var offered by rememberSaveable { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Re-evaluate after vault results; invalid_grant must not keep a dead offer.
    val faceIdArmed = viewModel.canFaceIdSignIn(vault) && vault.isAvailable()

    suspend fun runFaceIdSignIn() {
        if (promptRunning || state.isLoading) return
        promptRunning = true
        try {
            viewModel.signInWithFaceId(vault, strings.lockTitle, strings.lockSubtitle, strings.lockUsePassword)
        } finally {
            promptRunning = false
        }
    }

    LaunchedEffect(faceIdArmed) {
        if (faceIdArmed && !offered) {
            lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
            offered = true
            runFaceIdSignIn()
        }
    }
    LaunchedEffect(state.navigateTo) {
        when (state.navigateTo) {
            NavRoute.Home -> onNavigateToHome()
            NavRoute.PendingApproval -> onNavigateToPending()
            NavRoute.Rejected -> onNavigateToRejected()
            else -> Unit
        }
        if (state.navigateTo != null) viewModel.onNavigationHandled()
    }

    DnBackground(insetContent = true, glows = listOf(
        DnGlow(c.lime, 0.82f, -0.02f, 1.05f, 0.14f),
        DnGlow(c.blue, 0.03f, 0.74f, 0.82f, 0.08f),
    )) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Shared background owns auth insets: App.kt's NavHost is edge-to-edge.
            Spacer(Modifier.height(AuthLayout.topGap))
            Image(
                painterResource(Res.drawable.logo), "한마음 D+N",
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(c.textPrimary),
                modifier = Modifier.size(AuthLayout.logoWidth, AuthLayout.logoHeight),
            )
            Spacer(Modifier.height(AuthLayout.brandGap))
            Text("다시 만나서 반가워요", style = DnTheme.typography.titleLg, color = c.textPrimary,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(AppSpacing.sm))
            Text("한마음 D+N 계정으로 로그인하세요", style = DnTheme.typography.body,
                color = c.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(AuthLayout.actionGap))

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AuthLayout.contentGap)) {
                if (state.isLoading && !state.browserOpen) {
                    Text("사용자 정보를 확인하고 있어요.", style = DnTheme.typography.caption,
                        color = c.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else {
                    val noticeBody = when (notice) {
                        LoginRoute.NOTICE_VERIFY_EMAIL -> strings.noticeVerifyEmailBody
                        LoginRoute.NOTICE_REGISTERED -> strings.noticeRegisteredBody
                        else -> null
                    }
                    noticeBody?.let { LoginNotice(strings.noticeRegisteredTitle, it, false) }
                    state.error?.let {
                        LoginNotice(if (state.biometricExpired) "다시 로그인해 주세요." else "로그인을 완료하지 못했어요.", it, true)
                    }
                }
                DnPrimaryButton(
                    label = when {
                        state.isLoading -> "로그인 중…"
                        state.error != null -> "다시 시도"
                        else -> "계정으로 로그인"
                    },
                    onClick = { viewModel.onLoginClicked(browser, uiLocale = strings.languageTag) },
                    enabled = !state.isLoading && !promptRunning,
                    shape = DnInnerShape,
                    disabledContainer = c.surface2,
                    disabledContent = c.textSecondary,
                    modifier = Modifier.fillMaxWidth().heightIn(min = AuthLayout.actionHeight),
                )
                if (!state.isLoading || state.browserOpen) {
                    Text(strings.loginBrowserHelp,
                        style = DnTheme.typography.caption, color = c.textSecondary,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    if (faceIdArmed) {
                        DnTintedButton(
                            strings.loginSignInWithFaceId, { scope.launch { runFaceIdSignIn() } },
                            modifier = Modifier.fillMaxWidth().heightIn(min = AuthLayout.actionHeight),
                            icon = DnIcons.FaceId, tint = c.limeInk, container = c.limeDim,
                            enabled = !state.isLoading && !promptRunning, shape = DnInnerShape, showBorder = false,
                            iconSize = AuthLayout.iconSize,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = AuthLayout.linkTarget)
                            .clickable(enabled = !state.isLoading && !promptRunning, onClick = onRegisterClick),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm, Alignment.CenterHorizontally),
                    ) {
                        Text("아직 계정이 없으신가요?", style = DnTheme.typography.caption, color = c.textSecondary)
                        Text("회원가입", style = DnTheme.typography.captionStrong, color = c.limeInk)
                    }
                }
            }
            Spacer(Modifier.height(AppSpacing.xl))
        }
    }
}

@Composable
private fun LoginNotice(title: String, body: String, isError: Boolean) {
    val c = DnTheme.colors
    val ink = if (isError) c.red else c.limeInk
    Column(
        Modifier.fillMaxWidth().background(if (isError) c.redDim else c.limeDim, DnInnerShape).padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Text(title, style = DnTheme.typography.captionStrong, color = ink)
        Text(body, style = DnTheme.typography.caption, color = ink)
    }
}
