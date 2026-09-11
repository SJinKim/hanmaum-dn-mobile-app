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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
            LoadingDots()
        }
    }
}

/**
 * The three dots under the tagline, with the lime one walking left to right
 * and starting over — so the splash reads as "working", not as a still frame.
 *
 * A colour hand-off rather than a moving shape: each dot keeps its place and
 * only the lime passes along the row. The step is a spring, like every other
 * transition here, which keeps the hand-off soft instead of blinking.
 *
 * The loop ends on its own: the splash leaves composition as soon as the
 * session check has an answer, and the effect is cancelled with it.
 */
@Composable
private fun LoadingDots() {
    val c = DnTheme.colors
    var active by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(DOT_STEP_MS)
            active = (active + 1) % DOT_COUNT
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(DOT_COUNT) { i ->
            val fill by animateColorAsState(
                targetValue = if (i == active) c.lime else c.surface3,
                animationSpec = spring(),
                label = "splashDot",
            )
            Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(fill)
            )
        }
    }
}

private const val DOT_COUNT = 3

/** One dot per step; a full pass is three of these. */
private const val DOT_STEP_MS = 360L
