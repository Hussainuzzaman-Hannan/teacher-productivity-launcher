package com.teacher.productivitylauncher.presentation.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.productivitylauncher.data.local.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsDataStore = SettingsDataStore(application)

    // ── AppTheme state ───────────────────────────────────────
    private val _appTheme = MutableStateFlow(AppTheme.DARK)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    // ── backward compat — MainActivity এ লাগলে ──────────────
    val isDarkMode: StateFlow<Boolean>
        get() = MutableStateFlow(_appTheme.value != AppTheme.LIGHT).asStateFlow()

    // ── Notifications ────────────────────────────────────────
    private val _areNotificationsEnabled = MutableStateFlow(true)
    val areNotificationsEnabled: StateFlow<Boolean> = _areNotificationsEnabled.asStateFlow()

    init {
        loadThemePreference()
        loadNotificationPreference()
    }

    private fun loadThemePreference() {
        viewModelScope.launch {
            settingsDataStore.appTheme.collect { theme ->
                _appTheme.value = theme
            }
        }
    }

    private fun loadNotificationPreference() {
        viewModelScope.launch {
            settingsDataStore.areNotificationsEnabled.collect { enabled ->
                _areNotificationsEnabled.value = enabled
            }
        }
    }

    // ── Theme setters ────────────────────────────────────────
    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsDataStore.setAppTheme(theme)
            _appTheme.value = theme
        }
    }

    fun toggleDarkMode() {
        val next = if (_appTheme.value == AppTheme.LIGHT) AppTheme.DARK else AppTheme.LIGHT
        setAppTheme(next)
    }

    fun setDarkMode(enabled: Boolean) {
        setAppTheme(if (enabled) AppTheme.DARK else AppTheme.LIGHT)
    }

    // ── Notification setters ─────────────────────────────────
    fun toggleNotifications() {
        viewModelScope.launch {
            val newValue = !_areNotificationsEnabled.value
            settingsDataStore.setNotificationsEnabled(newValue)
            _areNotificationsEnabled.value = newValue
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotificationsEnabled(enabled)
            _areNotificationsEnabled.value = enabled
        }
    }
}