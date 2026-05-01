package com.teacher.productivitylauncher.presentation.appdrawer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.productivitylauncher.data.local.AppInfo
import com.teacher.productivitylauncher.data.local.AppRepository
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.RenamedApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppsDrawerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val database = TeacherDatabase.getDatabase(application)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            repository.getInstalledApps().collect { appList ->
                // Get renamed apps from database
                val renamedApps = database.renamedAppDao().getAllRenamed()
                val renamedMap = mutableMapOf<String, String>()

                renamedApps.collect { renamed ->
                    renamedMap.clear()
                    renamed.forEach {
                        renamedMap[it.packageName] = it.newName
                    }
                }

                val finalList = appList.map { app ->
                    if (renamedMap.containsKey(app.packageName)) {
                        app.copy(name = renamedMap[app.packageName] ?: app.name)
                    } else {
                        app
                    }
                }

                _apps.value = finalList
                filterApps()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterApps()
    }

    private fun filterApps() {
        val query = _searchQuery.value.lowercase()
        if (query.isEmpty()) {
            _filteredApps.value = _apps.value
        } else {
            _filteredApps.value = _apps.value.filter { app ->
                app.name.lowercase().contains(query) ||
                        app.packageName.lowercase().contains(query)
            }
        }
    }

    fun openApp(app: AppInfo) {
        repository.openApp(getApplication(), app.packageName)
    }

    fun refreshApps() {
        loadApps()
    }

    // Rename app function
    suspend fun renameApp(packageName: String, newName: String, originalName: String) {
        val renamedApp = RenamedApp(
            packageName = packageName,
            newName = newName,
            originalName = originalName
        )
        database.renamedAppDao().renameApp(renamedApp)
        loadApps() // Refresh the list
    }

    // Reset app name (remove rename)
    suspend fun resetAppName(packageName: String) {
        database.renamedAppDao().resetAppName(packageName)
        loadApps()
    }

    // Get renamed name if exists
    suspend fun getRenamedName(packageName: String): String? {
        return database.renamedAppDao().getRenamedApp(packageName)?.newName
    }
}