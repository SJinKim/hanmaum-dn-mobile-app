package com.hanmaum.dn.mobile.features.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 프로필") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProfileUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadProfile() }) { Text("다시 시도") }
                    }
                }
                is ProfileUiState.Success -> {
                    if (state.isEditing) {
                        ProfileEditContent(
                            state = state,
                            onPhoneChange = { viewModel.updatePhone(it) },
                            onImageUrlChange = { viewModel.updateImageUrl(it) },
                            onSave = { viewModel.saveProfile() },
                            onCancel = { viewModel.cancelEditing() },
                        )
                    } else {
                        ProfileViewContent(
                            profile = state.profile,
                            onEditClick = { viewModel.startEditing() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileViewContent(
    profile: MemberResponse,
    onEditClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "프로필 아이콘",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "${profile.lastName} ${profile.firstName}",
            style = MaterialTheme.typography.headlineSmall,
        )
        profile.churchRole?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.height(24.dp))
        ProfileField("이메일", profile.email ?: "—")
        ProfileField("전화번호", profile.phoneNumber ?: "—")
        ProfileField("지역", profile.city ?: "—")
        ProfileField("그룹", profile.groupName ?: "—")
        ProfileField("상태", profile.status.name)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onEditClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Edit, contentDescription = "프로필 수정")
            Spacer(Modifier.width(8.dp))
            Text("수정하기")
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider()
}

@Composable
private fun ProfileEditContent(
    state: ProfileUiState.Success,
    onPhoneChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("프로필 수정", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = state.editPhone,
            onValueChange = onPhoneChange,
            label = { Text("전화번호") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.editImageUrl,
            onValueChange = onImageUrlChange,
            label = { Text("프로필 이미지 URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        state.saveError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("취소") }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving,
            ) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp).semantics { contentDescription = "저장 중" }, strokeWidth = 2.dp)
                else Text("저장")
            }
        }
    }
}
