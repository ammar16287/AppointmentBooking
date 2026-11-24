package com.ammar.appointmentbooking.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ammar.appointmentbooking.data.Status

/** BookEase brand: deep indigo into violet, with a fresh teal accent. */
object BrandColors {
    val Indigo = Color(0xFF4338CA)
    val Violet = Color(0xFF7C3AED)
    val Teal = Color(0xFF14B8A6)
    val Ink = Color(0xFF1E1B4B)

    /** Diagonal gradient used for headers, the logo and hero cards. */
    val gradient: Brush get() = Brush.linearGradient(listOf(Indigo, Violet))

    /** Soft tints used to colour service avatars, cycled by index. */
    val tints = listOf(
        Color(0xFF4338CA), Color(0xFF0D9488), Color(0xFFDB2777),
        Color(0xFFEA580C), Color(0xFF7C3AED), Color(0xFF0284C7),
    )
}

private val LightColors = lightColorScheme(
    primary = BrandColors.Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = BrandColors.Ink,
    secondary = BrandColors.Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = BrandColors.Violet,
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF1F2F8),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceContainer = Color(0xFFF6F7FB),
    surfaceContainerLow = Color.White,
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = BrandColors.Ink,
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    tertiary = Color(0xFFC4B5FD),
    background = Color(0xFF0F1020),
    surface = Color(0xFF181A2E),
    surfaceVariant = Color(0xFF23263D),
    onSurfaceVariant = Color(0xFFA1A5BD),
    surfaceContainer = Color(0xFF14162A),
    outlineVariant = Color(0xFF2E3150),
)

private val AppTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

/** Status colours used across lists and calendars. */
fun Status.color(): Color = when (this) {
    Status.PENDING -> Color(0xFFD97706)
    Status.CONFIRMED -> Color(0xFF4338CA)
    Status.CHECKED_IN -> Color(0xFF7C3AED)
    Status.COMPLETED -> Color(0xFF059669)
    Status.CANCELLED -> Color(0xFF9CA3AF)
    Status.NO_SHOW -> Color(0xFFDC2626)
}
