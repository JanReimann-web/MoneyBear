package com.jan.moneybear.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

private fun createColorScheme(themeMode: String, accentId: String): androidx.compose.material3.ColorScheme {
    val palette = resolvePalette(themeMode)
    val accent = adjustAccent(resolveAccent(accentId), palette)
    val primary = accent
    val primaryContainer = if (palette.isDark) {
        primary.blendWith(Color.Black, 0.6f)
    } else {
        primary.blendWith(Color.White, 0.75f)
    }
    val secondary = if (palette.isDark) {
        primary.blendWith(Color.White, 0.5f)
    } else {
        primary.blendWith(palette.onBackground, 0.65f)
    }
    return if (palette.isDark) {
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
            background = palette.background,
            onBackground = palette.onBackground,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.onSurfaceVariant,
            outline = palette.outline,
            error = AccentRed,
            onError = Color.White,
            inverseSurface = palette.onBackground,
            inverseOnSurface = palette.surface,
            scrim = palette.surface.copy(alpha = 0.7f)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primaryContainer,
            onPrimaryContainer = palette.onBackground,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondary.blendWith(Color.White, 0.7f),
            onSecondaryContainer = palette.onBackground,
            tertiary = AccentAmber,
            onTertiary = Color.White,
            background = palette.background,
            onBackground = palette.onBackground,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.onSurfaceVariant,
            outline = palette.outline,
            error = AccentRed,
            onError = Color.White,
            inverseSurface = palette.onBackground,
            inverseOnSurface = palette.surface,
            scrim = palette.onBackground.copy(alpha = 0.1f)
        )
    }
}

private fun resolvePalette(themeMode: String): ThemePalette {
    val key = themeMode.lowercase(Locale.getDefault())
    return when (key) {
        "light", "beige" -> ThemePalette(
            background = BeigeBackground,
            surface = BeigeSurface,
            surfaceVariant = BeigeSurfaceVariant,
            onBackground = BeigeTextPrimary,
            onSurface = BeigeTextPrimary,
            onSurfaceVariant = BeigeTextSecondary,
            outline = BeigeOutline,
            isDark = false
        )
        "pink" -> ThemePalette(
            background = PinkBackground,
            surface = PinkSurface,
            surfaceVariant = PinkSurfaceVariant,
            onBackground = PinkTextPrimary,
            onSurface = PinkTextPrimary,
            onSurfaceVariant = PinkTextSecondary,
            outline = PinkOutline,
            isDark = false
        )
        "orange" -> ThemePalette(
            background = OrangeBackground,
            surface = OrangeSurface,
            surfaceVariant = OrangeSurfaceVariant,
            onBackground = OrangeTextPrimary,
            onSurface = OrangeTextPrimary,
            onSurfaceVariant = OrangeTextSecondary,
            outline = OrangeOutline,
            isDark = false
        )
        "purple" -> ThemePalette(
            background = PurpleBackground,
            surface = PurpleSurface,
            surfaceVariant = PurpleSurfaceVariant,
            onBackground = PurpleTextPrimary,
            onSurface = PurpleTextPrimary,
            onSurfaceVariant = PurpleTextSecondary,
            outline = PurpleOutline,
            isDark = false
        )
        "blue" -> ThemePalette(
            background = BlueBackground,
            surface = BlueSurface,
            surfaceVariant = BlueSurfaceVariant,
            onBackground = BlueTextPrimary,
            onSurface = BlueTextPrimary,
            onSurfaceVariant = BlueTextSecondary,
            outline = BlueOutline,
            isDark = true
        )
        "green", "dark" -> ThemePalette(
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = SurfaceVariantDark,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            onSurfaceVariant = TextSecondary,
            outline = BorderDefault,
            isDark = true
        )
        else -> ThemePalette(
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = SurfaceVariantDark,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            onSurfaceVariant = TextSecondary,
            outline = BorderDefault,
            isDark = true
        )
    }
}

private fun adjustAccent(accent: Color, palette: ThemePalette): Color {
    if (!palette.isDark) return accent
    return if (accent.luminance() < 0.2f) {
        accent.blendWith(Color.White, 0.45f)
    } else {
        accent
    }
}

private data class ThemePalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val isDark: Boolean
)

private fun resolveAccent(accentId: String): Color {
    val key = accentId.lowercase(Locale.getDefault())
    return when (key) {
        "teal" -> AccentTeal
        "orange" -> AccentOrange
        "purple" -> AccentPurple
        "green" -> AccentGreen
        "blue" -> AccentBlue
        "black" -> AccentBlack
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
