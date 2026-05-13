package com.teacher.productivitylauncher.presentation.widgets

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "widget_settings")

class WidgetStateManager(private val context: Context) {

    private val gson = Gson()
    private val WIDGETS_KEY = stringPreferencesKey("widgets_config")

    val widgets: Flow<List<WidgetConfig>> = context.widgetDataStore.data.map { prefs ->
        val json = prefs[WIDGETS_KEY] ?: return@map getDefaultWidgets()
        try {
            val type = object : TypeToken<List<WidgetConfig>>() {}.type
            gson.fromJson(json, type) ?: getDefaultWidgets()
        } catch (e: Exception) {
            getDefaultWidgets()
        }
    }

    suspend fun saveWidgets(widgets: List<WidgetConfig>) {
        context.widgetDataStore.edit { prefs ->
            prefs[WIDGETS_KEY] = gson.toJson(widgets)
        }
    }

    suspend fun updateWidget(updated: WidgetConfig) {
        context.widgetDataStore.edit { prefs ->
            val json = prefs[WIDGETS_KEY] ?: gson.toJson(getDefaultWidgets())
            val type = object : TypeToken<List<WidgetConfig>>() {}.type
            val list = gson.fromJson<List<WidgetConfig>>(json, type)?.toMutableList()
                ?: getDefaultWidgets().toMutableList()
            val index = list.indexOfFirst { it.id == updated.id }
            if (index >= 0) list[index] = updated else list.add(updated)
            prefs[WIDGETS_KEY] = gson.toJson(list)
        }
    }

    suspend fun removeWidget(widgetId: String) {
        context.widgetDataStore.edit { prefs ->
            val json = prefs[WIDGETS_KEY] ?: return@edit
            val type = object : TypeToken<List<WidgetConfig>>() {}.type
            val list = gson.fromJson<List<WidgetConfig>>(json, type)?.toMutableList()
                ?: return@edit
            list.removeAll { it.id == widgetId }
            prefs[WIDGETS_KEY] = gson.toJson(list)
        }
    }

    suspend fun reorderWidgets(widgets: List<WidgetConfig>) {
        context.widgetDataStore.edit { prefs ->
            prefs[WIDGETS_KEY] = gson.toJson(widgets)
        }
    }

    private fun getDefaultWidgets(): List<WidgetConfig> = listOf(
        WidgetConfig(
            id = "clock_default",
            type = WidgetType.CLOCK,
            size = WidgetSize.MEDIUM,
            order = 0,
            showOnHome = true
        ),
        WidgetConfig(
            id = "class_routine_default",
            type = WidgetType.CLASS_ROUTINE,
            size = WidgetSize.MEDIUM,
            order = 1,
            showOnHome = true
        ),
        WidgetConfig(
            id = "quick_notes_default",
            type = WidgetType.QUICK_NOTES,
            size = WidgetSize.SMALL,
            order = 2,
            showOnHome = true
        )
    )
}