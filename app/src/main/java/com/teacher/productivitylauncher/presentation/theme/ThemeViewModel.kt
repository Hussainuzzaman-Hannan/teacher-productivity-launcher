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

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _areNotificationsEnabled = MutableStateFlow(true)
    val areNotificationsEnabled: StateFlow<Boolean> = _areNotificationsEnabled.asStateFlow()

    init {
        loadThemePreference()
        loadNotificationPreference()
    }

    private fun loadThemePreference() {
        viewModelScope.launch {
            settingsDataStore.isDarkModeEnabled.collect { isDark ->
                _isDarkMode.value = isDark
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

    fun toggleDarkMode() {
        viewModelScope.launch {
            val newValue = !_isDarkMode.value
            settingsDataStore.setDarkModeEnabled(newValue)
            _isDarkMode.value = newValue
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDarkModeEnabled(enabled)
            _isDarkMode.value = enabled
        }
    }

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