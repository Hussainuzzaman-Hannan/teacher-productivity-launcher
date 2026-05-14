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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class AppsDrawerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val database = TeacherDatabase.getDatabase(application)
    private val context = application.applicationContext

    // ========== ক্যাশিং ==========
    private var cachedApps: List<AppInfo>? = null
    private var isCacheValid = false

    // প্রি-লোডিং এর জন্য করুটিন স্কোপ
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isFirstLoad = MutableStateFlow(true)
    val isFirstLoad: StateFlow<Boolean> = _isFirstLoad.asStateFlow()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    // BroadcastReceiver
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_REPLACED -> {
                    isCacheValid = false
                    repository.invalidateCache() // 🔥 নিচের AppRepository-তে এই মেথড যোগ করুন
                    refreshApps()
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(packageReceiver, filter)

        // 🔥 দ্রুততম প্রি-লোডিং (শুধু ১০টি অ্যাপ)
        startFastPreloading()

        // UI এর জন্য দ্রুত লোডিং শুরু করুন
        loadApps()
    }

    // 🔥 নতুন: দ্রুততম প্রি-লোডিং (প্রথম ১০টি অ্যাপ ১০০ms-এ)
    private fun startFastPreloading() {
        preloadScope.launch {
            try {
                android.util.Log.d("AppsDrawerViewModel", "⚡ Fast preloading started")

                val quickApps = withContext(Dispatchers.IO) {
                    repository.getInstalledApps(forceRefresh = false).first().take(10)
                }

                if (cachedApps == null && quickApps.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        if (_apps.value.isEmpty()) {
                            _apps.value = quickApps
                            filterApps()
                            _loadingProgress.value = 15
                        }
                    }
                }

                // সম্পূর্ণ লিস্ট ব্যাকগ্রাউন্ডে লোড
                loadAppsAsync()

            } catch (e: Exception) {
                android.util.Log.e("AppsDrawerViewModel", "Fast preloading error: ${e.message}")
            }
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            if (_isFirstLoad.value) {
                _isRefreshing.value = true
            }

            // ক্যাশ থাকলে তাৎক্ষণিক দেখান
            if (cachedApps != null && isCacheValid) {
                android.util.Log.d("AppsDrawerViewModel", "⚡ Loading from cache (instant)")
                _apps.value = cachedApps!!
                filterApps()
                _isRefreshing.value = false
                _isFirstLoad.value = false
                _loadingProgress.value = 100
            }
            // ক্যাশ না থাকলে, আল্ট্রা ফাস্ট লোড
            else {
                android.util.Log.d("AppsDrawerViewModel", "🔄 Ultra fast first load")
                loadUltraFast()
            }
        }
    }

    // 🔥 নতুন: আল্ট্রা ফাস্ট লোডিং (ডাটাবেস অপেক্ষা না করে)
    private suspend fun loadUltraFast() = withContext(Dispatchers.IO) {
        try {
            _loadingProgress.value = 5

            // প্রথমে শুধু অ্যাপ লিস্ট লোড করুন (ডাটাবেস ছাড়া)
            val rawApps = repository.getInstalledApps(forceRefresh = true).first()
            _loadingProgress.value = 30

            // ডাটাবেসের ডাটা সমান্তরালে লোড করুন
            val renamedAppsDeferred = async { database.renamedAppDao().getAllRenamed().first() }
            val hiddenAppsDeferred = async { database.hiddenAppDao().getAllHidden().first() }

            _loadingProgress.value = 50

            val renamedApps = renamedAppsDeferred.await()
            val hiddenApps = hiddenAppsDeferred.await()
            _loadingProgress.value = 70

            val renamedMap = renamedApps.associate { it.packageName to it.newName }
            val hiddenSet = hiddenApps.map { it.packageName }.toSet()

            val finalList = rawApps
                .filter { app -> app.packageName !in hiddenSet }
                .map { app ->
                    app.copy(name = renamedMap[app.packageName] ?: app.name)
                }

            cachedApps = finalList
            isCacheValid = true

            withContext(Dispatchers.Main) {
                _apps.value = finalList
                filterApps()
                _isRefreshing.value = false
                _isFirstLoad.value = false
                _loadingProgress.value = 100
                android.util.Log.d("AppsDrawerViewModel", "✅ Load complete: ${finalList.size} apps")
            }
        } catch (e: Exception) {
            android.util.Log.e("AppsDrawerViewModel", "Ultra fast load error: ${e.message}")
            withContext(Dispatchers.Main) {
                _isRefreshing.value = false
                _isFirstLoad.value = false
            }
        }
    }

    private suspend fun loadAppsAsync() = withContext(Dispatchers.IO) {
        try {
            if (cachedApps != null && isCacheValid) {
                return@withContext
            }

            val appList = repository.getInstalledApps(forceRefresh = false).first()
            val renamedApps = database.renamedAppDao().getAllRenamed().first()
            val hiddenApps = database.hiddenAppDao().getAllHidden().first()

            val renamedMap = renamedApps.associate { it.packageName to it.newName }
            val hiddenSet = hiddenApps.map { it.packageName }.toSet()

            val finalList = appList
                .filter { app -> app.packageName !in hiddenSet }
                .map { app ->
                    app.copy(name = renamedMap[app.packageName] ?: app.name)
                }

            if (cachedApps == null) {
                cachedApps = finalList
                isCacheValid = true
                withContext(Dispatchers.Main) {
                    if (_apps.value.size < finalList.size) {
                        _apps.value = finalList
                        filterApps()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppsDrawerViewModel", "Background load error: ${e.message}")
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
        isCacheValid = false
        repository.invalidateCache()
        loadApps()
    }

    suspend fun renameApp(packageName: String, newName: String, originalName: String) {
        val renamedApp = RenamedApp(
            packageName = packageName,
            newName = newName,
            originalName = originalName
        )
        database.renamedAppDao().renameApp(renamedApp)
        isCacheValid = false
        repository.invalidateCache()
        loadApps()
    }

    suspend fun resetAppName(packageName: String) {
        database.renamedAppDao().resetAppName(packageName)
        isCacheValid = false
        repository.invalidateCache()
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
        isCacheValid = false
        repository.invalidateCache()
        loadApps()
    }

    suspend fun unhideApp(packageName: String) {
        database.hiddenAppDao().unhideAppByPackage(packageName)
        isCacheValid = false
        repository.invalidateCache()
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