package com.hanmaum.dn.mobile.features.announcement.presentation

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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.presentation.components.ErrorView
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementDetailScreen(
    announcementId: String,
    onBackClick: () -> Unit
) {
    // Koin Injection mit Parametern

    val viewModel: AnnouncementDetailViewModel = koinViewModel(
        parameters = { parametersOf(announcementId) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") }, // Oder leer lassen
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    ErrorView(msg = state.error, onRetry = { viewModel.loadAnnouncement() })
                }
                state.announcement != null -> {
                    val item = state.announcement!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        // Kategorie Tag
                        Surface(
                            color = Color(item.getAnnouncementCategoryColor()).copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = item.getAnnouncementCategoryName(),
                                color = Color(item.getAnnouncementCategoryColor()),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Titel
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Datum
                        Text(
                            text = item.startAt, // Ggf. formatieren
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                        // Body (Inhalt)
                        Text(
                            text = item.body, // In DB heißt es 'body', im DTO 'content' - check dein Model!
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp
                        )
                    }
                }
            }
        }
    }
}