package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import hanmaumdnapp.composeapp.generated.resources.Res
import hanmaumdnapp.composeapp.generated.resources.PlusJakartaSans_Bold
import hanmaumdnapp.composeapp.generated.resources.PlusJakartaSans_ExtraBold
import hanmaumdnapp.composeapp.generated.resources.PlusJakartaSans_Medium
import hanmaumdnapp.composeapp.generated.resources.PlusJakartaSans_Regular
import hanmaumdnapp.composeapp.generated.resources.PlusJakartaSans_SemiBold
import org.jetbrains.compose.resources.Font

// Font family must be loaded inside a @Composable scope (CMP 1.7+ Resources API)
@Composable
fun rememberPlusJakartaSans(): FontFamily = FontFamily(
    Font(Res.font.PlusJakartaSans_Regular,   FontWeight.Normal),
    Font(Res.font.PlusJakartaSans_Medium,    FontWeight.Medium),
    Font(Res.font.PlusJakartaSans_SemiBold,  FontWeight.SemiBold),
    Font(Res.font.PlusJakartaSans_Bold,      FontWeight.Bold),
    Font(Res.font.PlusJakartaSans_ExtraBold, FontWeight.ExtraBold),
)

// Called inside AppTheme — builds the full Material3 Typography with Plus Jakarta Sans
@Composable
fun rememberAppTypography(): Typography {
    val ff = rememberPlusJakartaSans()
    return Typography(
        // Display: "Inspirational Statements" — tight letter-spacing (-0.02em)
        displayLarge = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.ExtraBold,
            fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-0.96).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Bold,
            fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.72).sp,
        ),
        displaySmall = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Bold,
            fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.56).sp,
        ),
        // Headline: section titles — pair with generous whitespace
        headlineLarge = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Bold,
            fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.1).sp,
        ),
        // Title: card headers and sub-sections
        titleLarge = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Medium,
            fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Medium,
            fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
        ),
        // Body: warm charcoal — never pure black (applied via onSurfaceVariant token)
        bodyLarge = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Normal,
            fontSize = 16.sp, lineHeight = 26.sp, letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Normal,
            fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 0.25.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Normal,
            fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.4.sp,
        ),
        // Label: buttons (SemiBold) + chips/categories (uppercase via call site)
        labelLarge = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Medium,
            fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Medium,
            fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
        ),
    )
}
