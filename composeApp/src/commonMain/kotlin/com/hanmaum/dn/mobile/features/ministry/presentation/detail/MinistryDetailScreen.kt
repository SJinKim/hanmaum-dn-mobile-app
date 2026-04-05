package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.presentation.components.ErrorView
import com.hanmaum.dn.mobile.features.ministry.domain.model.RegistrationStatus
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinistryDetailScreen(
    publicId: String,
    onBackClick: () -> Unit,
) {
    val viewModel: MinistryDetailViewModel = koinViewModel(parameters = { parametersOf(publicId) })
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("부서 상세") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is MinistryDetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is MinistryDetailUiState.Error -> ErrorView(msg = s.message, onRetry = { viewModel.load() })
                is MinistryDetailUiState.Success -> {
                    MinistryDetailContent(
                        state = s,
                        onRegisterClick = { viewModel.openSheet() },
                    )
                    if (s.showSheet) {
                        RegistrationBottomSheet(
                            note = s.noteInput,
                            isLoading = s.isRegistering,
                            error = s.registerError,
                            onNoteChange = { viewModel.updateNote(it) },
                            onConfirm = { viewModel.register() },
                            onDismiss = { viewModel.closeSheet() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MinistryDetailContent(
    state: MinistryDetailUiState.Success,
    onRegisterClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = state.detail.name,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        )
        state.detail.leaderName?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "리더: $it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = state.detail.longDescription ?: state.detail.shortDescription,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(32.dp))
        RegistrationButton(
            status = state.registrationStatus,
            onClick = onRegisterClick,
        )
    }
}

@Composable
private fun RegistrationButton(
    status: RegistrationStatus,
    onClick: () -> Unit,
) {
    when (status) {
        RegistrationStatus.NONE -> {
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text("신청하기")
            }
        }
        RegistrationStatus.PENDING -> {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("신청되었습니다")
            }
        }
        RegistrationStatus.APPROVED -> {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = Color(0xFF4CAF50),
                    disabledContentColor = Color.White,
                ),
            ) {
                Text("멤버입니다 ✓")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationBottomSheet(
    note: String,
    isLoading: Boolean,
    error: String?,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("부서 신청", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 500) onNoteChange(it) },
                label = { Text("자기소개 (선택)") },
                placeholder = { Text("리더에게 전달할 자기소개를 입력하세요") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                supportingText = { Text("${note.length}/500") },
            )
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("신청하기")
                }
            }
        }
    }
}
