package com.teacher.productivitylauncher.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Color Schemes ─────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary              = TealPrimary,
    onPrimary            = White,
    primaryContainer     = TealContainer,
    onPrimaryContainer   = OnTealContainer,

    secondary            = AmberSecondary,
    onSecondary          = White,
    secondaryContainer   = AmberContainer,
    onSecondaryContainer = OnAmberContainer,

    tertiary             = IndigoTertiary,
    onTertiary           = White,
    tertiaryContainer    = IndigoContainer,
    onTertiaryContainer  = OnIndigoContainer,

    background           = OffWhite,
    onBackground         = Color(0xFF0F1614),

    surface              = White,
    onSurface            = Color(0xFF0F1614),
    surfaceVariant       = Color(0xFFDAE5E2),
    onSurfaceVariant     = Color(0xFF3F4D4A),

    error                = ErrorRed,
    onError              = White,
    errorContainer       = ErrorContainer,
    onErrorContainer     = Color(0xFF410002),

    outline              = Color(0xFF6F7C79),
    outlineVariant       = Color(0xFFBECAC7),
    scrim                = Color(0xFF000000),
)

private val DarkColorScheme = darkColorScheme(
    primary              = TealPrimaryLight,
    onPrimary            = Color(0xFF003731),
    primaryContainer     = TealPrimaryDark,
    onPrimaryContainer   = TealContainer,

    secondary            = Color(0xFFFFB74D),
    onSecondary          = Color(0xFF4E2600),
    secondaryContainer   = Color(0xFF6D3900),
    onSecondaryContainer = AmberContainer,

    tertiary             = Color(0xFF9FA8DA),
    onTertiary           = Color(0xFF0D1478),
    tertiaryContainer    = Color(0xFF222E9A),
    onTertiaryContainer  = IndigoContainer,

    background           = DarkBackground,
    onBackground         = Color(0xFFE0EAE7),

    surface              = DarkSurface,
    onSurface            = Color(0xFFE0EAE7),
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = Color(0xFFBECAC7),

    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),

    outline              = Color(0xFF899390),
    outlineVariant       = DarkSurfaceVariant,
    scrim                = Color(0xFF000000),
)

private val AmoledColorScheme = darkColorScheme(
    primary              = TealPrimaryLight,
    onPrimary            = Color(0xFF003731),
    primaryContainer     = Color(0xFF004D40),
    onPrimaryContainer   = TealContainer,

    secondary            = Color(0xFFFFB74D),
    onSecondary          = Color(0xFF4E2600),
    secondaryContainer   = Color(0xFF3E1A00),
    onSecondaryContainer = AmberContainer,

    tertiary             = Color(0xFF9FA8DA),
    onTertiary           = Color(0xFF0D1478),
    tertiaryContainer    = Color(0xFF161E6E),
    onTertiaryContainer  = IndigoContainer,

    background           = AmoledBackground,
    onBackground         = Color(0xFFE0EAE7),

    surface              = AmoledSurface,
    onSurface            = Color(0xFFE0EAE7),
    surfaceVariant       = Color(0xFF0D1514),
    onSurfaceVariant     = Color(0xFFBECAC7),

    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),

    outline              = Color(0xFF4A5553),
    outlineVariant       = Color(0xFF1A2421),
    scrim                = Color(0xFF000000),
)

// ── Theme Enum (SettingsScreen এ ব্যবহারের জন্য) ─────────────
enum class AppTheme { LIGHT, DARK, AMOLED }

// ── Main Theme Composable ─────────────────────────────────────

@Composable
fun TeacherLauncherTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.LIGHT  -> LightColorScheme
        AppTheme.DARK   -> DarkColorScheme
        AppTheme.AMOLED -> AmoledColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            window?.let {
                it.statusBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars =
                    appTheme == AppTheme.LIGHT
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}