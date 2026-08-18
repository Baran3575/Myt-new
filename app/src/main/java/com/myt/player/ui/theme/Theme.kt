package com.myt.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Myt palette - dark with a green accent
val MytGreen = Color(0xFF1ED760)
val MytGreenDark = Color(0xFF169C46)
val BackgroundBlack = Color(0xFF121212)
val SurfaceDark = Color(0xFF181818)
val SurfaceElevated = Color(0xFF242424)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB3B3B3)

val MytColorScheme = darkColorScheme(
    primary = MytGreen,
    onPrimary = Color(0xFF04180C),
    primaryContainer = MytGreenDark,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = MytGreen,
    onSecondary = Color(0xFF04180C),
    background = BackgroundBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceElevated,
    surfaceContainerHighest = SurfaceElevated,
    error = Color(0xFFCF6679),
    outline = Color(0xFF3E3E3E)
)

val MytTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    )
)

@Composable
fun MytTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MytColorScheme,
        typography = MytTypography,
        content = content
    )
}