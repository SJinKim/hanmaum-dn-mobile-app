package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import hanmaumdnapp.composeapp.generated.resources.Res
import hanmaumdnapp.composeapp.generated.resources.pretendard_bold
import hanmaumdnapp.composeapp.generated.resources.pretendard_medium
import hanmaumdnapp.composeapp.generated.resources.pretendard_regular
import hanmaumdnapp.composeapp.generated.resources.pretendard_semibold
import org.jetbrains.compose.resources.Font

/**
 * Pretendard covers Latin and Hangul in one family — it is drawn on Inter for
 * Latin and Source Han Sans for Hangul. That matters here: the previous face
 * had no Hangul glyphs at all, so Korean fell back to a system font that
 * differs between Android and iOS.
 *
 * Because it is one family, the per-script weight step used in the Figma mock
 * (Hangul one step lighter than Latin) is not needed — that was a workaround
 * for pairing two separate families.
 */
@Composable
fun rememberPretendard(): FontFamily = FontFamily(
    Font(Res.font.pretendard_regular,  FontWeight.Normal),
    Font(Res.font.pretendard_medium,   FontWeight.Medium),
    Font(Res.font.pretendard_semibold, FontWeight.SemiBold),
    Font(Res.font.pretendard_bold,     FontWeight.Bold),
)

/** The ten styles the redesign actually uses. */
@Immutable
data class DnTypography(
    val display: TextStyle,
    val titleLg: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val stat: TextStyle,
    val bodyStrong: TextStyle,
    val body: TextStyle,
    val captionStrong: TextStyle,
    val caption: TextStyle,
    val label: TextStyle,
)

@Composable
fun rememberDnTypography(): DnTypography {
    val ff = rememberPretendard()
    return remember(ff) {
        DnTypography(
            display = TextStyle(
                fontFamily = ff, fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp,
            ),
            titleLg = TextStyle(
                fontFamily = ff, fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp,
            ),
            title = TextStyle(
                fontFamily = ff, fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp,
            ),
            headline = TextStyle(
                fontFamily = ff, fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp,
            ),
            stat = TextStyle(
                fontFamily = ff, fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.6).sp,
            ),
            bodyStrong = TextStyle(
                fontFamily = ff, fontWeight = FontWeight.Medium,
                fontSize = 15.sp, lineHeight = 22.sp,
            ),
            body = TextStyle(
                fontFamily = ff, fontWeight = FontWeight.Normal,
                fontSize = 15.sp, lineHeight = 22.sp,
            ),
            captionStrong = TextStyle(
                fontFamily = ff, fontWeight = FontWeight.Medium,
                fontSize = 13.sp, lineHeight = 18.sp,
            ),
            caption = TextStyle(
                fontFamily = ff, fontWeight = FontWeight.Normal,
                fontSize = 13.sp, lineHeight = 18.sp,
            ),
            label = TextStyle(
                fontFamily = ff, fontWeight = FontWeight.Medium,
                fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.3.sp,
            ),
        )
    }
}

/** Material slots, mapped onto the same ten styles so stock components match. */
fun DnTypography.toMaterialTypography(): Typography = Typography(
    displayLarge   = display,
    displayMedium  = display,
    displaySmall   = titleLg,
    headlineLarge  = titleLg,
    headlineMedium = title,
    headlineSmall  = headline,
    titleLarge     = title,
    titleMedium    = headline,
    titleSmall     = captionStrong,
    bodyLarge      = body,
    bodyMedium     = body,
    bodySmall      = caption,
    labelLarge     = bodyStrong,
    labelMedium    = captionStrong,
    labelSmall     = label,
)

val LocalDnTypography = staticCompositionLocalOf<DnTypography> {
    error("DnTypography not provided — wrap the content in AppTheme")
}

val DnTheme.typography: DnTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalDnTypography.current
