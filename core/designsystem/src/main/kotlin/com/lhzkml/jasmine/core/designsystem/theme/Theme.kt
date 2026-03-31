
package com.lhzkml.jasmine.core.designsystem.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Light default theme color scheme
 */
@VisibleForTesting
val LightDefaultColorScheme = lightColorScheme(
    primary = Mono10,
    onPrimary = Mono100,
    primaryContainer = Mono95,
    onPrimaryContainer = Mono10,
    secondary = Mono40,
    onSecondary = Mono100,
    secondaryContainer = Mono90,
    onSecondaryContainer = Mono10,
    tertiary = Mono60,
    onTertiary = Mono100,
    tertiaryContainer = Mono95,
    onTertiaryContainer = Mono10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Mono98,
    onBackground = Mono10,
    surface = Mono100,
    onSurface = Mono10,
    surfaceVariant = Mono95,
    onSurfaceVariant = Mono40,
    inverseSurface = Mono20,
    inverseOnSurface = Mono95,
    outline = Mono50, // Contrast for unselected thumb
    outlineVariant = Mono80, // Borders
    surfaceTint = Mono100,
    scrim = Mono10,
    surfaceBright = Mono100,
    surfaceContainer = Mono98,
    surfaceContainerHigh = Mono95,
    surfaceContainerHighest = Mono90, // Contrast for unselected track
    surfaceContainerLow = Mono98,
    surfaceContainerLowest = Mono100,
    surfaceDim = Mono80,
)

/**
 * Dark default theme color scheme
 */
@VisibleForTesting
val DarkDefaultColorScheme = darkColorScheme(
    primary = Mono100,
    onPrimary = Mono10,
    primaryContainer = Mono20,
    onPrimaryContainer = Mono95,
    secondary = Mono80,
    onSecondary = Mono10,
    secondaryContainer = Mono40,
    onSecondaryContainer = Mono90,
    tertiary = Mono90,
    onTertiary = Mono20,
    tertiaryContainer = Mono40,
    onTertiaryContainer = Mono95,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Mono10, // Dark grey background
    onBackground = Mono95,
    surface = Mono10,
    onSurface = Mono95,
    surfaceVariant = Mono20,
    onSurfaceVariant = Mono80,
    inverseSurface = Mono90,
    inverseOnSurface = Mono10,
    outline = Mono60, // Contrast for unselected thumb
    outlineVariant = Mono40, // Borders
    surfaceTint = Mono10,
    scrim = Mono100,
    surfaceBright = Mono20,
    surfaceContainer = Mono20,
    surfaceContainerHigh = Mono30,
    surfaceContainerHighest = Mono40, // Contrast for unselected track
    surfaceContainerLow = Mono10,
    surfaceContainerLowest = Mono10,
    surfaceDim = Mono10,
)



/**
 * Jasmine theme.
 *
 * @param darkTheme Whether the theme should use a dark color scheme (follows system by default).
 */
@Composable
fun JasmineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Color scheme
    val colorScheme = if (darkTheme) DarkDefaultColorScheme else LightDefaultColorScheme
    // Gradient colors
    val emptyGradientColors = GradientColors(container = colorScheme.surfaceColorAtElevation(2.dp))
    val defaultGradientColors = GradientColors(
        top = colorScheme.inverseOnSurface,
        bottom = colorScheme.primaryContainer,
        container = colorScheme.surface,
    )
    val gradientColors = defaultGradientColors
    // Background theme
    val defaultBackgroundTheme = BackgroundTheme(
        color = colorScheme.surface,
        tonalElevation = 2.dp,
    )
    val backgroundTheme = defaultBackgroundTheme
    val tintTheme = TintTheme()
    // Composition locals
    CompositionLocalProvider(
        LocalGradientColors provides gradientColors,
        LocalBackgroundTheme provides backgroundTheme,
        LocalTintTheme provides tintTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = JasmineTypography,
            content = content,
        )
    }
}
