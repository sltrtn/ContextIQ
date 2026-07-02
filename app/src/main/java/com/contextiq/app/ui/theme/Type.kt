package com.contextiq.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.contextiq.app.R

val ClashDisplay = FontFamily(
    Font(R.font.clash_display_extralight, FontWeight.W100),
    Font(R.font.clash_display_light, FontWeight.W300),
    Font(R.font.clash_display_regular, FontWeight.W400),
    Font(R.font.clash_display_medium, FontWeight.W500),
    Font(R.font.clash_display_semibold, FontWeight.W600),
    Font(R.font.clash_display_bold, FontWeight.W700),
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W700,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W700,
        fontSize = 45.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W600,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W600,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W600,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W700,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W700,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W700,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W700,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W700,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W700,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W700,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.W700,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
