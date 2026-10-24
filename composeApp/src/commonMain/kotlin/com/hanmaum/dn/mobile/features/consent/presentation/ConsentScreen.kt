package com.hanmaum.dn.mobile.features.consent.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTintedButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.theme.DnInnerShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentState
import com.hanmaum.dn.mobile.features.consent.presentation.components.ConsentNotice
import com.hanmaum.dn.mobile.features.consent.presentation.components.ConsentNoticeTone
import com.hanmaum.dn.mobile.features.consent.presentation.components.ConsentPolicyLink
import com.hanmaum.dn.mobile.features.consent.presentation.components.PrivacyConsentCheckbox
import org.koin.compose.viewmodel.koinViewModel

/**
 * The gate a signed-in member meets when APP_PRIVACY is missing or outdated
 * (Figma section 25, frames 03 and 05). There is no way past it but agreeing or
 * signing out; the server answers 403 to everything else meanwhile.
 */
@Composable
fun ConsentScreen(
    onContinue: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val viewModel: ConsentViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.navigateTo) {
        when (state.navigateTo) {
            ConsentNavigation.Continue -> onContinue()
            ConsentNavigation.LoggedOut -> onLoggedOut()
            // Withdrawing happens in 설정, never here.
            ConsentNavigation.Withdrawn, null -> return@LaunchedEffect
        }
        viewModel.onNavigationHandled()
    }

    DnBackground(glows = DnGlows.information()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = strings.consentNoticeTitle, onBack = null, actionIcon = null)

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val status = state.status
                when {
                    state.loadFailed -> {
                        ConsentNotice(
                            title = strings.consentLoadFailedTitle,
                            body = strings.consentLoadFailedBody,
                            tone = ConsentNoticeTone.Error,
                        )
                        DnPrimaryButton(
                            label = strings.consentReload,
                            onClick = viewModel::load,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !state.isLoading,
                            shape = DnInnerShape,
                        )
                    }
                    status != null -> {
                        val outdated = status.state == ConsentState.OUTDATED
                        Text(
                            text = if (outdated) strings.consentOutdatedTitle else strings.consentMissingTitle,
                            style = DnTheme.typography.headline.copy(fontSize = 20.sp),
                            color = c.textPrimary,
                        )
                        Text(
                            text = if (outdated) strings.consentOutdatedBody else strings.consentMissingBody,
                            style = DnTheme.typography.body.copy(fontSize = 15.sp, lineHeight = 22.sp),
                            color = c.textSecondary,
                        )
                        if (outdated) {
                            ConsentNotice(
                                title = strings.consentNewVersion(status.currentVersion),
                                body = status.grantedVersion?.let(strings::consentPreviousVersion).orEmpty(),
                                tone = ConsentNoticeTone.Amber,
                            )
                        }
                        if (state.saveFailed) {
                            ConsentNotice(
                                title = strings.consentSaveFailedTitle,
                                body = strings.consentSaveFailedBody,
                                tone = ConsentNoticeTone.Error,
                            )
                        }
                        PrivacyConsentCheckbox(
                            checked = state.accepted,
                            onCheckedChange = viewModel::onAcceptedChange,
                            enabled = !state.isSaving,
                        )
                        ConsentPolicyLink()
                        Spacer(Modifier.height(8.dp))
                        DnPrimaryButton(
                            label = if (state.saveFailed) strings.consentSaveAgain else strings.consentAgreeAndContinue,
                            onClick = viewModel::grant,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = state.canGrant,
                            shape = DnInnerShape,
                        )
                    }
                }
                // Signing out stays open while loading or failed: it needs no consent.
                DnTintedButton(
                    label = strings.profileLogout,
                    onClick = viewModel::logout,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    tint = c.limeInk,
                    container = c.limeDim,
                    shape = DnInnerShape,
                    showBorder = false,
                )
            }
        }
    }
}
