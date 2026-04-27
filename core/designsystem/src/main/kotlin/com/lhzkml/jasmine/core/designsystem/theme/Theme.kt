
package com.lhzkml.jasmine.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import android.view.View
import android.view.WindowInsetsController

/**
 * Light default theme color scheme
 */
@VisibleForTesting
val LightDefaultColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

/**
 * Dark default theme color scheme
 */
@VisibleForTesting
val DarkDefaultColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

// ==================== Custom Colors ====================

@Immutable
data class CustomColors(
    val appTitleGradientColors: List<Color> = listOf(),
    val tabHeaderBgColor: Color = Color.Transparent,
    val taskCardBgColor: Color = Color.Transparent,
    val taskBgColors: List<Color> = listOf(),
    val taskBgGradientColors: List<List<Color>> = listOf(),
    val taskIconColors: List<Color> = listOf(),
    val taskIconShapeBgColor: Color = Color.Transparent,
    val homeBottomGradient: List<Color> = listOf(),
    val userBubbleBgColor: Color = Color.Transparent,
    val agentBubbleBgColor: Color = Color.Transparent,
    val linkColor: Color = Color.Transparent,
    val successColor: Color = Color.Transparent,
    val recordButtonBgColor: Color = Color.Transparent,
    val waveFormBgColor: Color = Color.Transparent,
    val modelInfoIconColor: Color = Color.Transparent,
    val warningContainerColor: Color = Color.Transparent,
    val warningTextColor: Color = Color.Transparent,
    val errorContainerColor: Color = Color.Transparent,
    val errorTextColor: Color = Color.Transparent,
    val newFeatureContainerColor: Color = Color.Transparent,
    val newFeatureTextColor: Color = Color.Transparent,
    val bgStarColor: Color = Color.Transparent,
    val promoBannerBgBrush: Brush = Brush.verticalGradient(listOf(Color.Transparent)),
    val promoBannerIconBgBrush: Brush = Brush.verticalGradient(listOf(Color.Transparent)),
)

val LocalCustomColors = staticCompositionLocalOf { CustomColors() }

val lightCustomColors = CustomColors(
    appTitleGradientColors = listOf(Color(0xFF85B1F8), Color(0xFF3174F1)),
    tabHeaderBgColor = Color(0xFF3174F1),
    taskCardBgColor = surfaceContainerLowestLight,
    taskBgColors = listOf(
        Color(0xFFFFF5F5),
        Color(0xFFF4FBF6),
        Color(0xFFF1F6FE),
        Color(0xFFFFFBF0),
    ),
    taskBgGradientColors = listOf(
        listOf(Color(0xFFE25F57), Color(0xFFDB372D)),
        listOf(Color(0xFF41A15F), Color(0xFF128937)),
        listOf(Color(0xFF669DF6), Color(0xFF3174F1)),
        listOf(Color(0xFFFDD45D), Color(0xFFCAA12A)),
    ),
    taskIconColors = listOf(
        Color(0xFFDB372D),
        Color(0xFF128937),
        Color(0xFF3174F1),
        Color(0xFFCAA12A),
    ),
    taskIconShapeBgColor = Color.White,
    homeBottomGradient = listOf(Color(0x00F8F9FF), Color(0xFFFFEFC9)),
    agentBubbleBgColor = Color(0xFFE9EEF6),
    userBubbleBgColor = Color(0xFF32628D),
    linkColor = Color(0xFF32628D),
    successColor = Color(0xFF3D860B),
    recordButtonBgColor = Color(0xFFEE675C),
    waveFormBgColor = Color(0xFFAAAAAA),
    modelInfoIconColor = Color(0xFFCCCCCC),
    warningContainerColor = Color(0xFFFEF7E0),
    warningTextColor = Color(0xFFE37400),
    errorContainerColor = Color(0xFFFCE8E6),
    errorTextColor = Color(0xFFD93025),
    newFeatureContainerColor = Color(0xFFEEDCFE),
    newFeatureTextColor = Color(0xFF400B84),
    bgStarColor = Color(0x3A669AF5),
    promoBannerBgBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to Color(0x42ACB7FF),
            0.6154f to Color(0x422D96FF),
            1.0f to Color(0x423C6BFF),
        ),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
    ),
    promoBannerIconBgBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.2442f to Color(0x3B446EFF),
            0.4296f to Color(0x3B2E96FF),
            0.6651f to Color(0x3BB1C5FF),
        ),
        start = Offset(0f, 1f),
        end = Offset(1f, 0f),
    ),
)

val darkCustomColors = CustomColors(
    appTitleGradientColors = listOf(Color(0xFF85B1F8), Color(0xFF3174F1)),
    tabHeaderBgColor = Color(0xFF3174F1),
    taskCardBgColor = surfaceContainerHighDark,
    taskBgColors = listOf(
        Color(0xFF181210),
        Color(0xFF131711),
        Color(0xFF191924),
        Color(0xFF1A1813),
    ),
    taskBgGradientColors = listOf(
        listOf(Color(0xFFE25F57), Color(0xFFDB372D)),
        listOf(Color(0xFF41A15F), Color(0xFF128937)),
        listOf(Color(0xFF669DF6), Color(0xFF3174F1)),
        listOf(Color(0xFFFDD45D), Color(0xFFCAA12A)),
    ),
    taskIconColors = listOf(
        Color(0xFFE25F57),
        Color(0xFF41A15F),
        Color(0xFF669DF6),
        Color(0xFFCAA12A),
    ),
    taskIconShapeBgColor = Color(0xFF202124),
    homeBottomGradient = listOf(Color(0x00F8F9FF), Color(0x1AF6AD01)),
    agentBubbleBgColor = Color(0xFF1B1C1D),
    userBubbleBgColor = Color(0xFF1F3760),
    linkColor = Color(0xFF9DCAFC),
    successColor = Color(0xFFA1CE83),
    recordButtonBgColor = Color(0xFFEE675C),
    waveFormBgColor = Color(0xFFAAAAAA),
    modelInfoIconColor = Color(0xFFCCCCCC),
    warningContainerColor = Color(0xFF554C33),
    warningTextColor = Color(0xFFFCC934),
    errorContainerColor = Color(0xFF523A3B),
    errorTextColor = Color(0xFFEE675C),
    newFeatureContainerColor = Color(0xFFEEDCFE),
    newFeatureTextColor = Color(0xFF400B84),
    bgStarColor = Color(0x19346BF0),
    promoBannerBgBrush = Brush.linearGradient(
        colorStops = arrayOf(0.0f to Color(0x82183570), 0.8077f to Color(0x820A122D)),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
    ),
    promoBannerIconBgBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.2442f to Color(0x6F0F41F8),
            0.4296f to Color(0x6F1685F8),
            0.6651f to Color(0x6F809EF3),
        ),
        start = Offset(0f, 1f),
        end = Offset(1f, 0f),
    ),
)

val MaterialTheme.customColors: CustomColors
    @Composable @ReadOnlyComposable get() = LocalCustomColors.current

/**
 * Controls the color of the phone's status bar icons.
 */
@Composable
fun StatusBarColorController(useDarkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(useDarkTheme) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.insetsController
                if (useDarkTheme) {
                    controller?.setSystemBarsAppearance(
                        0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    controller?.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                if (!useDarkTheme) {
                    view.systemUiVisibility = view.systemUiVisibility or
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    view.systemUiVisibility = view.systemUiVisibility and
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
        }
    }
}

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
    val view = LocalView.current

    StatusBarColorController(useDarkTheme = darkTheme)

    val colorScheme = if (darkTheme) DarkDefaultColorScheme else LightDefaultColorScheme
    val customColorsPalette = if (darkTheme) darkCustomColors else lightCustomColors

    // Keep backward compat locals
    val gradientColors = GradientColors(
        top = colorScheme.inverseOnSurface,
        bottom = colorScheme.primaryContainer,
        container = colorScheme.surface,
    )
    val backgroundTheme = BackgroundTheme(
        color = colorScheme.surface,
        tonalElevation = 2.dp,
    )
    val tintTheme = TintTheme()

    CompositionLocalProvider(
        LocalCustomColors provides customColorsPalette,
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

    // Keep navigation bar transparent on theme changes.
    LaunchedEffect(darkTheme) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }
}
