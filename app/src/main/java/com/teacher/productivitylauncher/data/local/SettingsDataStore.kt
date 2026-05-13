package com.teacher.productivitylauncher.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.teacher.productivitylauncher.presentation.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val DARK_MODE_KEY      = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS_KEY  = booleanPreferencesKey("notifications_enabled")
        val APP_THEME_KEY      = stringPreferencesKey("app_theme")
    }

    // ── পুরনো dark mode (backward compat) ───────────────────
    val isDarkModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DARK_MODE_KEY] ?: false }

    // ── নতুন AppTheme (LIGHT / DARK / AMOLED) ───────────────
    val appTheme: Flow<AppTheme> = context.dataStore.data
        .map { preferences ->
            when (preferences[APP_THEME_KEY]) {
                AppTheme.LIGHT.name  -> AppTheme.LIGHT
                AppTheme.AMOLED.name -> AppTheme.AMOLED
                else                 -> AppTheme.DARK   // default
            }
        }

    val areNotificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFICATIONS_KEY] ?: true }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME_KEY] = theme.name
            // backward compat
            preferences[DARK_MODE_KEY] = theme != AppTheme.LIGHT
        }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
            preferences[APP_THEME_KEY] = if (enabled) AppTheme.DARK.name else AppTheme.LIGHT.name
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }
}