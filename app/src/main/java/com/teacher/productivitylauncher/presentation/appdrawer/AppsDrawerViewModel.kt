package com.teacher.productivitylauncher.presentation.appdrawer

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.productivitylauncher.data.local.AppInfo
import com.teacher.productivitylauncher.data.local.AppRepository
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.BlockedApp
import com.teacher.productivitylauncher.data.local.entity.HiddenApp
import com.teacher.productivitylauncher.data.local.entity.RenamedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Package change receiver
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_REPLACED -> {
                    AppRepository.invalidateCache()
                    loadApps(forceRefresh = true)
                }
            }
        }
    }

    init {
        context.registerReceiver(packageReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        })

        // 🔥 একটাই call, cache থাকলে instant
        loadApps()
    }

    fun loadApps(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true

            withContext(Dispatchers.IO) {
                try {
                    // সমান্তরালে সব কিছু লোড করো
                    val rawAppsDeferred = async {
                        repository.getInstalledApps(forceRefresh).first()
                    }
                    val renamedDeferred = async {
                        database.renamedAppDao().getAllRenamed().first()
                    }
                    val hiddenDeferred = async {
                        database.hiddenAppDao().getAllHidden().first()
                    }

                    val rawApps = rawAppsDeferred.await()
                    val renamedMap = renamedDeferred.await()
                        .associate { it.packageName to it.newName }
                    val hiddenSet = hiddenDeferred.await()
                        .map { it.packageName }.toSet()

                    val finalList = rawApps
                        .filter { it.packageName !in hiddenSet }
                        .map { app -> app.copy(name = renamedMap[app.packageName] ?: app.name) }

                    withContext(Dispatchers.Main) {
                        _apps.value = finalList
                        filterApps()
                        _isLoading.value = false
                        android.util.Log.d("AppsDrawerViewModel", "✅ Loaded ${finalList.size} apps")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppsDrawerViewModel", "Load error: ${e.message}")
                    withContext(Dispatchers.Main) { _isLoading.value = false }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterApps()
    }

    private fun filterApps() {
        val query = _searchQuery.value.lowercase()
        _filteredApps.value = if (query.isEmpty()) _apps.value
        else _apps.value.filter {
            it.name.lowercase().contains(query) || it.packageName.lowercase().contains(query)
        }
    }

    fun openApp(app: AppInfo) = repository.openApp(getApplication(), app.packageName)

    fun refreshApps() = loadApps(forceRefresh = true)

    suspend fun renameApp(packageName: String, newName: String, originalName: String) {
        database.renamedAppDao().renameApp(RenamedApp(packageName, newName, originalName))
        AppRepository.invalidateCache()
        loadApps(forceRefresh = true)
    }

    suspend fun resetAppName(packageName: String) {
        database.renamedAppDao().resetAppName(packageName)
        AppRepository.invalidateCache()
        loadApps(forceRefresh = true)
    }

    suspend fun getRenamedName(packageName: String): String? =
        database.renamedAppDao().getRenamedApp(packageName)?.newName

    suspend fun blockApp(packageName: String, appName: String) {
        database.blockedAppDao().blockApp(BlockedApp(packageName, appName))
    }

    suspend fun unblockApp(packageName: String) {
        database.blockedAppDao().unblockAppByPackage(packageName)
    }

    suspend fun hideApp(packageName: String, appName: String) {
        database.hiddenAppDao().hideApp(HiddenApp(packageName, appName))
        AppRepository.invalidateCache()
        loadApps(forceRefresh = true)
    }

    suspend fun unhideApp(packageName: String) {
        database.hiddenAppDao().unhideAppByPackage(packageName)
        AppRepository.invalidateCache()
        loadApps(forceRefresh = true)
    }

    override fun onCleared() {
        super.onCleared()
        try { context.unregisterReceiver(packageReceiver) } catch (e: Exception) { }
    }
}