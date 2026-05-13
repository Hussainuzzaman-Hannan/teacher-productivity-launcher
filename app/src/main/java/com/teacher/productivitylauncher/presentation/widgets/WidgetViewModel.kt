package com.teacher.productivitylauncher.presentation.widgets

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.widgetDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "widget_settings")

class WidgetViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val gson = Gson()
    private val WIDGETS_KEY = stringPreferencesKey("widgets_config")
    private val QUOTE_INDEX_KEY = intPreferencesKey("quote_index")

    private val _widgets = MutableStateFlow<List<WidgetConfig>>(emptyList())
    val widgets: StateFlow<List<WidgetConfig>> = _widgets.asStateFlow()

    private val _quoteIndex = MutableStateFlow(0)
    val quoteIndex: StateFlow<Int> = _quoteIndex.asStateFlow()

    init {
        loadWidgets()
        loadQuoteIndex()
    }

    private fun loadWidgets() {
        viewModelScope.launch {
            context.widgetDataStore.data.map { prefs ->
                val json = prefs[WIDGETS_KEY] ?: return@map getDefaultWidgets()
                try {
                    val type = object : TypeToken<List<WidgetConfig>>() {}.type
                    gson.fromJson(json, type) ?: getDefaultWidgets()
                } catch (e: Exception) {
                    getDefaultWidgets()
                }
            }.collect { widgetsList ->
                _widgets.value = widgetsList.sortedBy { it.order }
            }
        }
    }

    private fun loadQuoteIndex() {
        viewModelScope.launch {
            context.widgetDataStore.data.map { prefs ->
                prefs[QUOTE_INDEX_KEY] ?: 0
            }.collect { index ->
                _quoteIndex.value = index
            }
        }
    }

    fun updateQuoteIndex(index: Int) {
        viewModelScope.launch {
            context.widgetDataStore.edit { prefs ->
                prefs[QUOTE_INDEX_KEY] = index
            }
            _quoteIndex.value = index
        }
    }

    fun addWidget(type: WidgetType) {
        viewModelScope.launch {
            val currentList = _widgets.value.toMutableList()
            val newWidget = WidgetConfig(
                id = "${type.name}_${System.currentTimeMillis()}",
                type = type,
                size = WidgetSize.MEDIUM,
                order = currentList.size,
                showOnHome = true
            )
            currentList.add(newWidget)
            saveWidgets(currentList)
        }
    }

    fun updateWidget(widget: WidgetConfig) {
        viewModelScope.launch {
            val currentList = _widgets.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == widget.id }
            if (index >= 0) {
                currentList[index] = widget
                saveWidgets(currentList)
            }
        }
    }

    fun removeWidget(widgetId: String) {
        viewModelScope.launch {
            val currentList = _widgets.value.toMutableList()
            currentList.removeAll { it.id == widgetId }
            currentList.forEachIndexed { idx, widget ->
                currentList[idx] = widget.copy(order = idx)
            }
            saveWidgets(currentList)
        }
    }

    fun toggleHomeVisibility(widget: WidgetConfig) {
        viewModelScope.launch {
            updateWidget(widget.copy(showOnHome = !widget.showOnHome))
        }
    }

    fun moveUp(index: Int) {
        if (index <= 0) return
        viewModelScope.launch {
            val list = _widgets.value.toMutableList()
            val temp = list[index]
            list[index] = list[index - 1].copy(order = index)
            list[index - 1] = temp.copy(order = index - 1)
            saveWidgets(list)
        }
    }

    fun moveDown(index: Int) {
        val list = _widgets.value
        if (index >= list.size - 1) return
        viewModelScope.launch {
            val mutableList = list.toMutableList()
            val temp = mutableList[index]
            mutableList[index] = mutableList[index + 1].copy(order = index)
            mutableList[index + 1] = temp.copy(order = index + 1)
            saveWidgets(mutableList)
        }
    }

    fun reorderWidgets(widgets: List<WidgetConfig>) {
        viewModelScope.launch {
            saveWidgets(widgets)
        }
    }

    private suspend fun saveWidgets(widgets: List<WidgetConfig>) {
        context.widgetDataStore.edit { prefs ->
            prefs[WIDGETS_KEY] = gson.toJson(widgets)
        }
        _widgets.value = widgets
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

private fun stringPreferencesKey(name: String): Preferences.Key<String> {
    return androidx.datastore.preferences.core.stringPreferencesKey(name)
}