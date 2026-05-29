package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import hanmaumdnapp.composeapp.generated.resources.Res
import hanmaumdnapp.composeapp.generated.resources.Pretendard_Regular
import hanmaumdnapp.composeapp.generated.resources.Pretendard_SemiBold
import hanmaumdnapp.composeapp.generated.resources.Pretendard_Bold
import hanmaumdnapp.composeapp.generated.resources.Pretendard_ExtraBold
import hanmaumdnapp.composeapp.generated.resources.Pretendard_Black
import org.jetbrains.compose.resources.Font

@Composable
fun rememberPretendard(): FontFamily = FontFamily(
    Font(Res.font.Pretendard_Regular,   FontWeight.Normal),
    Font(Res.font.Pretendard_SemiBold,  FontWeight.SemiBold),
    Font(Res.font.Pretendard_Bold,      FontWeight.Bold),
    Font(Res.font.Pretendard_ExtraBold, FontWeight.ExtraBold),
    Font(Res.font.Pretendard_Black,     FontWeight.Black),
)

@Composable
fun rememberAppTypography(): Typography {
    val ff = rememberPretendard()
    return remember(ff) {
        Typography(
            displayLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Black,     fontSize = 32.sp, lineHeight = 38.sp,  letterSpacing = (-1.2).sp),
            displayMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.Black,     fontSize = 28.sp, lineHeight = 34.sp,  letterSpacing = (-1.0).sp),
            displaySmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp,  letterSpacing = (-0.8).sp),
            headlineLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.8).sp),
            headlineMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.6).sp),
            headlineSmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Bold,      fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.4).sp),
            titleLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Bold,     fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = (-0.4).sp),
            titleMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = (-0.2).sp),
            titleSmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = (-0.1).sp),
            bodyLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.4.sp, letterSpacing = 0.sp),
            bodyMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 19.2.sp, letterSpacing = 0.sp),
            bodySmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 17.6.sp, letterSpacing = 0.sp),
            labelLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 2.0.sp),
            labelMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.Bold, fontSize = 9.sp,  lineHeight = 13.sp, letterSpacing = 1.5.sp),
            labelSmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 8.sp, lineHeight = 12.sp, letterSpacing = 1.0.sp),
        )
    }
}
