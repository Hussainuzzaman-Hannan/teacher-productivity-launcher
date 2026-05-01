package com.teacher.productivitylauncher.presentation

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teacher.productivitylauncher.R
import com.teacher.productivitylauncher.presentation.launcher.LauncherScreen
import com.teacher.productivitylauncher.presentation.theme.TeacherLauncherTheme
import com.teacher.productivitylauncher.presentation.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply normal theme after splash screen
        setTheme(R.style.Theme_TeacherProductivityLauncher)
        super.onCreate(savedInstanceState)

        // Make status bar and navigation bar transparent
        setupEdgeToEdge()

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()

            // Update status bar icons based on theme
            UpdateStatusBarIcons(isDarkMode = isDarkMode)

            TeacherLauncherTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LauncherScreen()
                }
            }
        }
    }

    private fun setupEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

                // Set status bar color to transparent
                statusBarColor = android.graphics.Color.TRANSPARENT

                // Set navigation bar color to transparent
                navigationBarColor = android.graphics.Color.TRANSPARENT
            }
        }

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    @Composable
    private fun UpdateStatusBarIcons(isDarkMode: Boolean) {
        SideEffect {
            val window = this@MainActivity.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            // For status bar icons: dark icons for light theme, light icons for dark theme
            // isDarkMode = true -> Dark theme -> Light icons (isAppearanceLightStatusBars = false)
            // isDarkMode = false -> Light theme -> Dark icons (isAppearanceLightStatusBars = true)
            insetsController.isAppearanceLightStatusBars = !isDarkMode
            insetsController.isAppearanceLightNavigationBars = !isDarkMode
        }
    }
}