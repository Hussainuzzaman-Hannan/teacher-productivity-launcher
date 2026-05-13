package com.teacher.productivitylauncher.presentation.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// DataStore extension
private val Context.dataStore by preferencesDataStore(name = "home_screen_settings")

// Preference keys
private val GRID_COLUMNS = intPreferencesKey("grid_columns")
private val ICON_SIZE = intPreferencesKey("icon_size")
private val SHOW_LABELS = booleanPreferencesKey("show_labels")
private val LABEL_SIZE = intPreferencesKey("label_size")
private val HORIZONTAL_SPACING = intPreferencesKey("horizontal_spacing")
private val VERTICAL_SPACING = intPreferencesKey("vertical_spacing")
private val SHOW_FAVORITE_TITLE = booleanPreferencesKey("show_favorite_title")

data class HomeScreenSettings(
    val gridColumns: Int = 4,
    val iconSize: Int = 56,
    val showLabels: Boolean = true,
    val labelSize: Int = 11,
    val horizontalSpacing: Int = 12,
    val verticalSpacing: Int = 0,
    val showFavoriteTitle: Boolean = true
) {
    companion object {
        fun default() = HomeScreenSettings()
    }
}

// Settings Manager with DataStore persistence
class HomeScreenSettingsManager(private val context: Context) {

    private val dataStore = context.dataStore

    // Read settings from DataStore as Flow
    val settingsFlow: Flow<HomeScreenSettings> = dataStore.data
        .catch { exception ->
            emit(androidx.datastore.preferences.core.emptyPreferences())
        }
        .map { preferences ->
            HomeScreenSettings(
                gridColumns = preferences[GRID_COLUMNS] ?: 4,
                iconSize = preferences[ICON_SIZE] ?: 56,
                showLabels = preferences[SHOW_LABELS] ?: true,
                labelSize = preferences[LABEL_SIZE] ?: 11,
                horizontalSpacing = preferences[HORIZONTAL_SPACING] ?: 12,
                verticalSpacing = preferences[VERTICAL_SPACING] ?: 0,
                showFavoriteTitle = preferences[SHOW_FAVORITE_TITLE] ?: true
            )
        }

    suspend fun updateGridColumns(columns: Int) {
        dataStore.edit { preferences ->
            preferences[GRID_COLUMNS] = columns.coerceIn(2, 6)
        }
    }

    suspend fun updateIconSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[ICON_SIZE] = size.coerceIn(40, 80)
        }
    }

    suspend fun updateShowLabels(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_LABELS] = show
        }
    }

    suspend fun updateLabelSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[LABEL_SIZE] = size.coerceIn(8, 16)
        }
    }

    suspend fun updateHorizontalSpacing(spacing: Int) {
        dataStore.edit { preferences ->
            preferences[HORIZONTAL_SPACING] = spacing.coerceIn(4, 24)
        }
    }

    suspend fun updateShowFavoriteTitle(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_FAVORITE_TITLE] = show
        }
    }

    suspend fun resetToDefault() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

// State holder for Composable
class HomeScreenSettingsState(context: Context) {
    private val manager = HomeScreenSettingsManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _settings = MutableStateFlow(HomeScreenSettings.default())
    val settings: StateFlow<HomeScreenSettings> = _settings.asStateFlow()

    init {
        scope.launch {
            manager.settingsFlow.collect { newSettings ->
                _settings.value = newSettings
            }
        }
    }

    fun updateGridColumns(columns: Int) {
        scope.launch {
            manager.updateGridColumns(columns)
        }
    }

    fun updateIconSize(size: Int) {
        scope.launch {
            manager.updateIconSize(size)
        }
    }

    fun updateShowLabels(show: Boolean) {
        scope.launch {
            manager.updateShowLabels(show)
        }
    }

    fun updateLabelSize(size: Int) {
        scope.launch {
            manager.updateLabelSize(size)
        }
    }

    fun updateHorizontalSpacing(spacing: Int) {
        scope.launch {
            manager.updateHorizontalSpacing(spacing)
        }
    }

    fun updateShowFavoriteTitle(show: Boolean) {
        scope.launch {
            manager.updateShowFavoriteTitle(show)
        }
    }

    fun resetToDefault() {
        scope.launch {
            manager.resetToDefault()
        }
    }
}

@Composable
fun rememberHomeScreenSettingsState(): HomeScreenSettingsState {
    val context = LocalContext.current
    return remember { HomeScreenSettingsState(context) }
}