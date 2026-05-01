package com.teacher.productivitylauncher.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun TeacherLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = PrimaryLightBlue,
            secondary = PrimaryLightBlue,
            tertiary = PrimaryLightBlue,
            background = DarkBackground,
            surface = DarkSurface,
            onPrimary = White,
            onBackground = White,
            onSurface = White
        )
    } else {
        lightColorScheme(
            primary = PrimaryBlue,
            secondary = PrimaryBlue,
            tertiary = PrimaryBlue,
            background = OffWhite,
            surface = White,
            onPrimary = White,
            onBackground = Black,
            onSurface = Black
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}