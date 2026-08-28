package com.hanmaum.dn.mobile.features.pending.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlow
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.pending.presentation.SplashViewModel
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import hanmaumdnapp.composeapp.generated.resources.Res
import hanmaumdnapp.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.graphics.ColorFilter
import org.koin.compose.viewmodel.koinViewModel

/**
 * Splash. The logo already reads "D†N · DANIEL&NEHEMIA", so the wordmark
 * below it was dropped — only the tagline remains.
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel = koinViewModel(),
    onNavigate: (NavRoute) -> Unit,
) {
    val destination by viewModel.navigateTo.collectAsState()
    val c = DnTheme.colors

    LaunchedEffect(destination) {
        destination?.let { route ->
            onNavigate(route)
            viewModel.onNavigationHandled()
        }
    }

    DnBackground(glows = listOf(DnGlow(c.lime, 0.5f, 0.55f, 1.2f, 0.16f))) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "한마음 D+N",
                contentScale = ContentScale.Fit,
                // the artwork is black; tint it so it survives the dark canvas
                colorFilter = ColorFilter.tint(c.textPrimary),
                modifier = Modifier.width(176.dp),
            )

            Spacer(Modifier.height(22.dp))
            Text("함께 걷는 신앙 공동체", style = DnTheme.typography.caption, color = c.textTertiary)

            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (i == 0) c.lime else c.surface3)
                    )
                }
            }
        }
    }
}
