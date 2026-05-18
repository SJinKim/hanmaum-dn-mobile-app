package com.hanmaum.dn.mobile.features.album.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    pcloudCode: String,
    albumName: String,
    onPhotoClick: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: AlbumDetailViewModel = koinViewModel { parametersOf(pcloudCode, albumName) },
) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(albumName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columns = when {
                maxWidth < 600.dp -> 3
                maxWidth < 900.dp -> 4
                else -> 5
            }
            when (state) {
                AlbumDetailUiState.Loading -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                is AlbumDetailUiState.Error -> {
                    val e = state as AlbumDetailUiState.Error
                    Box(
                        Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(e.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is AlbumDetailUiState.Success -> {
                    val s = state as AlbumDetailUiState.Success
                    if (s.items.isEmpty()) {
                        Box(
                            Modifier.fillMaxSize().padding(padding),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(strings.albumEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentPadding = PaddingValues(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(s.items, key = { it.fileId }) { item ->
                                val url = s.resolvedUrls[item.fileId]
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clickable(enabled = url != null) { url?.let { onPhotoClick(it) } },
                                ) {
                                    if (url != null) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = item.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        ) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
