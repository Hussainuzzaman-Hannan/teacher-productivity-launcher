package com.teacher.productivitylauncher.presentation.appdrawer

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.productivitylauncher.data.local.AppInfo
import com.teacher.productivitylauncher.data.local.AppRepository
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.BlockedApp
import com.teacher.productivitylauncher.data.local.entity.HiddenApp
import com.teacher.productivitylauncher.data.local.entity.RenamedApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppsDrawerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val database = TeacherDatabase.getDatabase(application)
    private val context = application.applicationContext

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // BroadcastReceiver for app install/uninstall events
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    android.util.Log.d("AppsDrawerViewModel", "Package added")
                    refreshApps()
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    android.util.Log.d("AppsDrawerViewModel", "Package removed")
                    refreshApps()
                }
                Intent.ACTION_PACKAGE_REPLACED -> {
                    android.util.Log.d("AppsDrawerViewModel", "Package replaced")
                    refreshApps()
                }
            }
        }
    }

    init {
        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(packageReceiver, filter)

        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadAppsAsync()
            _isRefreshing.value = false
        }
    }

    private suspend fun loadAppsAsync() = withContext(Dispatchers.IO) {
        try {
            val appList = repository.getInstalledApps().first()
            val renamedApps = database.renamedAppDao().getAllRenamed().first()
            val hiddenApps = database.hiddenAppDao().getAllHidden().first()

            val renamedMap = renamedApps.associate { it.packageName to it.newName }
            val hiddenSet = hiddenApps.map { it.packageName }.toSet()

            val finalList = appList
                .filter { app -> app.packageName !in hiddenSet }
                .map { app ->
                    app.copy(name = renamedMap[app.packageName] ?: app.name)
                }

            _apps.value = finalList
            filterApps()
        } catch (e: Exception) {
            android.util.Log.e("AppsDrawerViewModel", "Error loading apps: ${e.message}")
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterApps()
    }

    private fun filterApps() {
        val query = _searchQuery.value.lowercase()
        _filteredApps.value = if (query.isEmpty()) {
            _apps.value
        } else {
            _apps.value.filter { app ->
                app.name.lowercase().contains(query) ||
                        app.packageName.lowercase().contains(query)
            }
        }
    }

    fun openApp(app: AppInfo) {
        repository.openApp(getApplication(), app.packageName)
    }

    fun refreshApps() {
        android.util.Log.d("AppsDrawerViewModel", "Manual refresh triggered")
        loadApps()
    }

    suspend fun renameApp(packageName: String, newName: String, originalName: String) {
        val renamedApp = RenamedApp(
            packageName = packageName,
            newName = newName,
            originalName = originalName
        )
        database.renamedAppDao().renameApp(renamedApp)
        loadApps()
    }

    suspend fun resetAppName(packageName: String) {
        database.renamedAppDao().resetAppName(packageName)
        loadApps()
    }

    suspend fun getRenamedName(packageName: String): String? {
        return database.renamedAppDao().getRenamedApp(packageName)?.newName
    }

    suspend fun blockApp(packageName: String, appName: String) {
        val blockedApp = BlockedApp(
            packageName = packageName,
            appName = appName
        )
        database.blockedAppDao().blockApp(blockedApp)
    }

    suspend fun unblockApp(packageName: String) {
        database.blockedAppDao().unblockAppByPackage(packageName)
    }

    suspend fun hideApp(packageName: String, appName: String) {
        val hiddenApp = HiddenApp(
            packageName = packageName,
            appName = appName
        )
        database.hiddenAppDao().hideApp(hiddenApp)
        loadApps()
    }

    suspend fun unhideApp(packageName: String) {
        database.hiddenAppDao().unhideAppByPackage(packageName)
        loadApps()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}