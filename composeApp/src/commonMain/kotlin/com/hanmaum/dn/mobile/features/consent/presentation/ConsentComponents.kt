package com.hanmaum.dn.mobile.features.consent.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTintedButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.AppSize
import com.hanmaum.dn.mobile.core.presentation.theme.AppSpacing
import com.hanmaum.dn.mobile.core.presentation.theme.ConsentLayout
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnInnerShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentProblem
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentStatus
import com.hanmaum.dn.mobile.features.consent.domain.model.PolicyConsentState
import com.hanmaum.dn.mobile.features.consent.domain.model.PrivacyPolicyLink
import com.hanmaum.dn.mobile.features.login.presentation.components.DEV_TEAM_EMAIL
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun ConsentNotice(message: String, error: Boolean = false, warning: Boolean = false) {
    val c = DnTheme.colors
    Text(
        message, style = DnTheme.typography.caption,
        color = if (error) c.red else if (warning) c.amber else c.textSecondary,
        modifier = Modifier.fillMaxWidth().clip(DnInnerShape)
            .background(if (error) c.redDim else if (warning) c.amberDim else c.surface)
            .padding(AppSpacing.md),
    )
}

@Composable
internal fun ConsentLink(label: String, enabled: Boolean = true, color: Color = DnTheme.colors.limeInk, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = AppSize.touchTarget)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, style = DnTheme.typography.captionStrong,
            color = if (enabled) color else DnTheme.colors.textTertiary)
    }
}

@Composable
internal fun PrivacyPolicyButton(policyLink: PrivacyPolicyLink) {
    val s = LocalStrings.current
    val uri = LocalUriHandler.current
    var failed by remember { mutableStateOf(false) }
    ConsentLink(s.consentPolicy, enabled = policyLink.isAvailable) {
        failed = runCatching { uri.openUri(checkNotNull(policyLink.url)) }.isFailure
    }
    if (failed) ConsentNotice(s.consentLinkFailed, error = true)
}

/** The link is a separate tap target; opening it never toggles acceptance. */
@Composable
fun PolicyConsent(
    state: PolicyConsentState,
    policyLink: PrivacyPolicyLink,
    busy: Boolean,
    onAccepted: (Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    val s = LocalStrings.current
    val c = DnTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(s.consentTitle, style = DnTheme.typography.bodyStrong, color = c.textPrimary)
        if (state.isLoading) {
            ConsentNotice(s.consentLoading)
        } else {
            state.problem?.let { ConsentNotice(s.consentProblem(it), error = it != ConsentProblem.VERSION_OUTDATED, warning = it == ConsentProblem.VERSION_OUTDATED) }
            if (state.policy != null && policyLink.isAvailable) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = AppSize.touchTarget)
                        .toggleable(state.accepted, enabled = !busy, role = Role.Checkbox, onValueChange = onAccepted),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ConsentLayout.rowGap),
                ) {
                    Box(Modifier.size(AppSize.touchTarget), contentAlignment = Alignment.Center) {
                        val shape = RoundedCornerShape(ConsentLayout.checkboxRadius)
                        Box(
                            Modifier.size(ConsentLayout.checkbox).clip(shape)
                                .background(if (state.accepted) c.lime else c.surface2)
                                .then(if (state.accepted) Modifier else Modifier.border(
                                    androidx.compose.foundation.BorderStroke(ConsentLayout.checkboxStroke, c.strokeStrong), shape)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.accepted) Icon(DnIcons.Check, null, tint = c.onLime,
                                modifier = Modifier.size(ConsentLayout.checkIcon))
                        }
                    }
                    Text(s.consentLabel, style = DnTheme.typography.bodyStrong, color = c.textPrimary,
                        modifier = Modifier.weight(1f))
                }
                Text(s.consentDetail, style = DnTheme.typography.caption, color = c.textSecondary)
                Text(s.consentVersion(state.policy.version), style = DnTheme.typography.caption, color = c.textTertiary)
            }
            if (state.policy == null && state.problem != ConsentProblem.POLICY_UNAVAILABLE) {
                ConsentLink(s.consentRetry, enabled = !busy, onClick = onRetry)
            }
        }
        PrivacyPolicyButton(policyLink)
        Text(s.consentWithdrawalHint, style = DnTheme.typography.caption, color = c.textSecondary)
    }
}

@Composable
fun ConsentGate(
    viewModel: ConsentViewModel,
    activeDestination: Boolean,
    onSignedOut: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(activeDestination) { viewModel.checkSession(activeDestination) }
    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
    if (state.phase == ConsentPhase.READY && viewModel.hasCurrentSession()) {
        content()
        return
    }
    val strings = LocalStrings.current
    val s = strings
    val c = DnTheme.colors
    DnBackground(insetContent = true) {
        Column(Modifier.fillMaxSize()) {
            DnTopBar(s.consentTitle, onBack = null, actionIcon = null)
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                when (state.phase) {
                    ConsentPhase.LOADING -> ConsentNotice(s.consentLoading)
                    ConsentPhase.ERROR -> {
                        ConsentNotice(s.consentLoadFailed, error = true)
                        DnPrimaryButton(s.consentRetry, viewModel::refresh, Modifier.fillMaxWidth(),
                            enabled = !state.isRefreshing, shape = DnInnerShape)
                    }
                    ConsentPhase.REQUIRED -> {
                        val outdated = state.summary?.privacy?.state == ConsentStatus.OUTDATED ||
                            state.selection.problem == ConsentProblem.VERSION_OUTDATED
                        Text(if (outdated) s.consentOutdatedTitle else s.consentMissingTitle,
                            style = DnTheme.typography.title, color = c.textPrimary)
                        Text(if (outdated) s.consentOutdatedBody else s.consentMissingBody,
                            style = DnTheme.typography.body, color = c.textSecondary)
                        if (outdated && state.selection.policy != null) {
                            val current = s.consentNewVersion(checkNotNull(state.selection.policy).version)
                            val previous = state.summary?.privacy?.grantedVersion?.let { s.consentPreviousVersion(it) }
                            ConsentNotice(listOfNotNull(current, previous).joinToString("\n"), warning = true)
                        }
                        val selection = if (outdated && state.selection.problem == ConsentProblem.VERSION_OUTDATED) {
                            state.selection.copy(problem = null)
                        } else state.selection
                        PolicyConsent(selection, viewModel.policyLink, state.isBusy || state.isRefreshing, viewModel::setAccepted, viewModel::retryPolicy)
                        DnPrimaryButton(
                            if (state.isBusy || state.isRefreshing) s.consentSaving else s.consentContinueLabel, viewModel::grant,
                            Modifier.fillMaxWidth(), enabled = state.selection.canSubmit && !state.isBusy && !state.isRefreshing,
                            shape = DnInnerShape,
                        )
                    }
                    ConsentPhase.READY -> Unit
                }
                DnTintedButton(strings.profileLogout, viewModel::logout, Modifier.fillMaxWidth(),
                    tint = c.limeInk, container = c.limeDim, shape = DnInnerShape,
                    enabled = !state.isBusy, showBorder = false)
            }
        }
    }
}

@Composable
fun ConsentSettings(viewModel: ConsentViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val s = strings
    val c = DnTheme.colors
    val uri = LocalUriHandler.current
    var contactFailed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.refresh() }
    Column(Modifier.padding(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        when {
            state.isRefreshing -> ConsentNotice(s.consentLoading)
            state.settingsProblem != null -> {
                ConsentNotice(s.consentProblem(checkNotNull(state.settingsProblem)), error = true)
                ConsentLink(s.consentRetry, enabled = !state.isBusy, onClick = viewModel::refresh)
            }
            else -> {
                Text(s.consentGranted, style = DnTheme.typography.captionStrong, color = c.textPrimary)
                state.summary?.privacy?.let { consent ->
                    val date = remember(consent.grantedAt, strings.languageTag) {
                        consent.grantedAt?.toLocalDateTime(TimeZone.of("Europe/Berlin"))?.date?.toString()
                            ?.let { value -> if (strings.languageTag == "ko") value.replace('-', '.') else value }
                    }
                    // Missing proof stays unknown; no invented date or device timestamp.
                    Text(s.consentRecord(date ?: "—", consent.grantedVersion ?: "—"),
                        style = DnTheme.typography.caption, color = c.textSecondary)
                }
            }
        }
        PrivacyPolicyButton(viewModel.policyLink)
        ConsentLink(s.consentWithdraw, enabled = !state.isBusy && !state.isRefreshing,
            color = c.red, onClick = viewModel::requestWithdrawal)
        ConsentLink(s.consentContact) {
            contactFailed = runCatching { uri.openUri("mailto:$DEV_TEAM_EMAIL") }.isFailure
        }
        if (contactFailed) ConsentNotice(s.consentLinkFailed, error = true)
        Text(s.consentController, style = DnTheme.typography.caption, color = c.textSecondary)
        SelectionContainer { Text(DEV_TEAM_EMAIL, style = DnTheme.typography.caption, color = c.textSecondary) }
    }
    if (state.showWithdrawConfirmation) {
        Dialog(onDismissRequest = viewModel::dismissWithdrawal) {
            Column(
                Modifier.fillMaxWidth().clip(DnCardShape).background(c.surface).padding(AppSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                Box(Modifier.size(ConsentLayout.dialogIcon).clip(DnInnerShape).background(c.redDim),
                    contentAlignment = Alignment.Center) {
                    Icon(DnIcons.AlertTriangle, null, tint = c.red, modifier = Modifier.size(ConsentLayout.checkbox))
                }
                Text(s.consentWithdrawTitle, style = DnTheme.typography.headline, color = c.textPrimary, textAlign = TextAlign.Center)
                Text(s.consentWithdrawBody, style = DnTheme.typography.caption, color = c.textSecondary, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    DnTintedButton(strings.cancel, viewModel::dismissWithdrawal, Modifier.weight(1f),
                        tint = c.textPrimary, container = c.surface2, enabled = !state.isBusy, showBorder = false)
                    DnTintedButton(if (state.isBusy) s.consentSaving else s.consentWithdraw, viewModel::confirmWithdrawal,
                        Modifier.weight(1f), enabled = !state.isBusy, showBorder = false)
                }
            }
        }
    }
}
