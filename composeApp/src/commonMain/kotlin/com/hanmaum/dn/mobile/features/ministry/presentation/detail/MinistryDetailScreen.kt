package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.presentation.components.DnErrorState
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.ministry.domain.model.RegistrationStatus
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 사역 detail. The registration button reads 신청하기, matching the 양육 page —
 * the two pages are the same promise, so they use the same word.
 */
@Composable
fun MinistryDetailScreen(
    publicId: String,
    onBackClick: () -> Unit,
) {
    val viewModel: MinistryDetailViewModel = koinViewModel(parameters = { parametersOf(publicId) })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = DnTheme.colors

    DnBackground(glows = DnGlows.action()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = "사역", onBack = onBackClick, onAction = { })

            when (val s = state) {
                is MinistryDetailUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = c.lime)
                }

                is MinistryDetailUiState.Error ->
                    DnErrorState(onRetry = viewModel::load)

                is MinistryDetailUiState.Success -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(18.dp))

                    DetailHero(
                        icon = DnIcons.Users,
                        container = c.limeDim,
                        ink = c.limeInk,
                        eyebrow = "함께 섬기는 자리",
                        name = s.detail.name,
                    )

                    Spacer(Modifier.height(14.dp))
                    Text(
                        s.detail.shortDescription,
                        style = DnTheme.typography.body,
                        color = c.textSecondary,
                    )

                    Spacer(Modifier.height(20.dp))
                    // MinistryDto also carries schedules and contacts; the client
                    // model does not map them yet, so only the leader is shown.
                    DetailFacts(
                        rows = listOf(
                            Triple(DnIcons.User, "리더", s.detail.leaderName ?: "미정"),
                            Triple(DnIcons.Users, "상태", if (s.detail.isActive) "모집 중" else "모집 마감"),
                        )
                    )

                    Spacer(Modifier.height(22.dp))
                    DetailSection(
                        title = "우리의 마음",
                        body = s.detail.longDescription ?: s.detail.shortDescription,
                    )

                    Spacer(Modifier.height(28.dp))

                    when (s.registrationStatus) {
                        RegistrationStatus.NONE -> DnPrimaryButton(
                            label = "신청하기",
                            leading = DnIcons.UserCheck,
                            onClick = viewModel::openSheet,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        RegistrationStatus.PENDING -> StatusPill("신청 완료 · 승인 대기", c.amberDim, c.amber)
                        RegistrationStatus.APPROVED -> StatusPill("이미 함께하고 있습니다", c.limeDim, c.limeInk)
                    }

                    Spacer(Modifier.height(60.dp))
                }
            }
        }
    }

    val success = state as? MinistryDetailUiState.Success
    if (success?.showSheet == true) {
        RegistrationSheet(
            note = success.noteInput,
            isLoading = success.isRegistering,
            error = success.registerError,
            onNoteChange = viewModel::updateNote,
            onConfirm = viewModel::register,
            onDismiss = viewModel::closeSheet,
        )
    }
}

@Composable
private fun StatusPill(label: String, container: androidx.compose.ui.graphics.Color, ink: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(DnTileShape)
            .background(container, DnTileShape)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(DnIcons.Check, null, tint = ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(label, style = DnTheme.typography.bodyStrong, color = ink)
    }
}
