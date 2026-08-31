package com.hanmaum.dn.mobile.features.album.presentation.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnErrorState
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnImagePlaceholder
import com.hanmaum.dn.mobile.core.presentation.components.DnDock
import com.hanmaum.dn.mobile.core.presentation.components.DnScrollEdge
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import org.koin.compose.viewmodel.koinViewModel

/**
 * 앨범. Two columns of covers.
 *
 * The year filter from the design is not built: AlbumDto carries no date,
 * so the chips would have nothing to filter on (hanmaum-dn-server#116).
 */
@Composable
fun AlbumsScreen(
    onBackClick: () -> Unit,
    onAlbumClick: (pcloudCode: String, albumName: String) -> Unit,
    viewModel: AlbumsViewModel = koinViewModel(),
) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = DnTheme.colors

    DnBackground(glows = DnGlows.information()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = strings.navAlbum, onBack = onBackClick)

            when (val s = state) {
                AlbumsUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = c.lime)
                }

                is AlbumsUiState.Error ->
                    DnErrorState(onRetry = viewModel::load)

                is AlbumsUiState.Success -> if (s.albums.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text(strings.albumsEmpty, style = DnTheme.typography.body, color = c.textSecondary)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = DnDock.contentInset()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(s.albums, key = { it.album.publicId }) { summary ->
                            Column(
                                Modifier.clickable {
                                    onAlbumClick(summary.album.pcloudCode, summary.album.name)
                                },
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                val cover = summary.coverUrl
                                if (cover != null) {
                                    AsyncImage(
                                        model = cover,
                                        contentDescription = summary.album.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(DnTileShape)
                                            .border(1.dp, c.strokeSubtle, DnTileShape),
                                    )
                                } else {
                                    DnImagePlaceholder(
                                        Modifier.fillMaxWidth().aspectRatio(1f),
                                        cornerRadius = 24.dp,
                                    )
                                }
                                Column {
                                    Text(
                                        summary.album.name,
                                        style = DnTheme.typography.captionStrong,
                                        color = c.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    summary.photoCount?.let {
                                        Text(
                                            "${it}장",
                                            style = DnTheme.typography.caption,
                                            color = c.textTertiary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        DnScrollEdge()
    }
}
