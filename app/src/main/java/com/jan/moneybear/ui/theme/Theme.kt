package com.jan.moneybear.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import java.util.Locale

@Composable
fun MoneyBearTheme(
    themeMode: String,
    accentId: String,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(themeMode, accentId) {
        createColorScheme(themeMode = themeMode, accentId = accentId)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun createColorScheme(themeMode: String, accentId: String) =
    if (themeMode.lowercase(Locale.getDefault()) == "light") {
        val accent = resolveAccent(accentId)
        val primary = accent
        val primaryContainer = primary.blendWith(Color.White, 0.75f)
        val secondary = primary.blendWith(Color.Black, 0.65f)
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primaryContainer,
            onPrimaryContainer = LightTextPrimary,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondary.blendWith(Color.White, 0.7f),
            onSecondaryContainer = LightTextPrimary,
            tertiary = AccentAmber,
            onTertiary = Color.White,
            background = LightBackground,
            onBackground = LightTextPrimary,
            surface = LightSurface,
            onSurface = LightTextPrimary,
            surfaceVariant = SurfaceVariantLight,
            onSurfaceVariant = LightTextSecondary,
            outline = OutlineLight,
            error = AccentRed,
            onError = Color.White,
            inverseSurface = LightTextPrimary,
            inverseOnSurface = LightSurface,
            scrim = LightTextPrimary.copy(alpha = 0.1f)
        )
    } else {
        val accent = resolveAccent(accentId)
        val primary = accent
        val primaryContainer = primary.blendWith(Color.Black, 0.6f)
        val secondary = primary.blendWith(Color.White, 0.5f)
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primaryContainer,
            onPrimaryContainer = Color.White,
            secondary = secondary,
            onSecondary = Color.Black,
            secondaryContainer = secondary.blendWith(Color.Black, 0.6f),
            onSecondaryContainer = Color.White,
            tertiary = AccentAmber,
            onTertiary = Color.Black,
            background = DarkBackground,
            onBackground = TextPrimary,
            surface = DarkSurface,
            onSurface = TextPrimary,
            surfaceVariant = SurfaceVariantDark,
            onSurfaceVariant = TextSecondary,
            outline = BorderDefault,
            error = AccentRed,
            onError = Color.White,
            inverseSurface = TextPrimary,
            inverseOnSurface = DarkSurface,
            scrim = DarkSurface.copy(alpha = 0.7f)
        )
    }

private fun resolveAccent(accentId: String): Color {
    val key = accentId.lowercase(Locale.getDefault())
    return when (key) {
        "teal" -> AccentTeal
        "orange" -> AccentOrange
        "purple" -> AccentPurple
        "green" -> AccentGreen
        "blue" -> AccentBlue
        else -> AccentTeal
    }
}

private fun Color.blendWith(other: Color, ratio: Float): Color {
    val clamped = ratio.coerceIn(0f, 1f)
    val inverse = 1f - clamped
    return Color(
        red = red * inverse + other.red * clamped,
        green = green * inverse + other.green * clamped,
        blue = blue * inverse + other.blue * clamped,
        alpha = alpha * inverse + other.alpha * clamped
    )
}
