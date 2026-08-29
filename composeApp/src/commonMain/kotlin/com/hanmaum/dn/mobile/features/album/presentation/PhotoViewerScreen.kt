package com.hanmaum.dn.mobile.features.album.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hanmaum.dn.mobile.core.presentation.components.DnGlassIconButton
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme

/**
 * Full-screen photo. Stays on a dark ground in both themes — a light
 * backdrop competes with the image, which is why photo viewers are dark
 * everywhere.
 */
@Composable
fun PhotoViewerScreen(photoUrl: String, onBackClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(DnTheme.colors.mediaBackdrop),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(20.dp),
        ) {
            DnGlassIconButton(DnIcons.ArrowLeft, "뒤로", onBackClick)
        }
    }
}
