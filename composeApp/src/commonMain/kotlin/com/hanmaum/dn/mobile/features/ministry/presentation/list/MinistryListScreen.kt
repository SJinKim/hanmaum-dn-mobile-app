package com.hanmaum.dn.mobile.features.ministry.presentation.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.presentation.components.ErrorView
import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinistryListScreen(
    onBackClick: () -> Unit,
    onMinistryClick: (String) -> Unit,
) {
    val viewModel: MinistryListViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("부서") },
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
                is MinistryListUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is MinistryListUiState.Error -> ErrorView(msg = s.message, onRetry = { viewModel.loadMinistries() })
                is MinistryListUiState.Success -> {
                    if (s.ministries.isEmpty()) {
                        Text(
                            "등록된 부서가 없습니다",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.outline,
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(s.ministries, key = { it.publicId }) { ministry ->
                                MinistryCard(ministry = ministry, onClick = { onMinistryClick(ministry.publicId) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinistryCard(
    ministry: Ministry,
    onClick: () -> Unit,
) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier              = Modifier.padding(20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape    = MaterialTheme.shapes.small,
                color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector        = Icons.Default.Group,
                    contentDescription = null,
                    modifier           = Modifier.padding(10.dp),
                    tint               = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier              = Modifier.weight(1f),
                verticalArrangement   = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text     = ministry.name,
                    style    = MaterialTheme.typography.titleMedium,
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ministry.leaderName?.let {
                    Text(
                        text  = "리더: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text     = ministry.shortDescription,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
