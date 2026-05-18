package com.teacher.productivitylauncher.data.local

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    companion object {
        // 🔥 Application-level cache — ViewModel destroy হলেও টিকে থাকে
        @Volatile private var cachedAppList: List<AppInfo>? = null
        @Volatile private var lastCacheTime = 0L
        private const val CACHE_DURATION = 5 * 60 * 1000L // 5 মিনিট

        fun invalidateCache() {
            cachedAppList = null
            lastCacheTime = 0L
            android.util.Log.d("AppRepository", "Cache invalidated")
        }

        fun isCacheValid(): Boolean {
            return cachedAppList != null &&
                    (System.currentTimeMillis() - lastCacheTime) < CACHE_DURATION
        }
    }

    fun getInstalledApps(forceRefresh: Boolean = false): Flow<List<AppInfo>> = flow {
        val apps = withContext(Dispatchers.IO) {
            if (!forceRefresh && isCacheValid()) {
                android.util.Log.d("AppRepository", "⚡ Cache hit (${cachedAppList?.size} apps)")
                cachedAppList!!
            } else {
                android.util.Log.d("AppRepository", "🔄 Loading fresh app list")
                getAppListFast().also {
                    cachedAppList = it
                    lastCacheTime = System.currentTimeMillis()
                }
            }
        }
        emit(apps)
    }

    private fun getAppListFast(): List<AppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return try {
            val resolvedActivities = packageManager.queryIntentActivities(intent, 0)
            android.util.Log.d("AppRepository", "✅ Found ${resolvedActivities.size} apps")
            resolvedActivities.mapNotNull { resolveInfo ->
                try {
                    AppInfo(
                        name = resolveInfo.loadLabel(packageManager).toString(),
                        packageName = resolveInfo.activityInfo.packageName,
                        icon = resolveInfo.loadIcon(packageManager),
                        activityName = resolveInfo.activityInfo.name
                    )
                } catch (e: Exception) { null }
            }.sortedBy { it.name }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Fast method failed: ${e.message}")
            getAppListFallback()
        }
    }

    private fun getAppListFallback(): List<AppInfo> {
        val packageManager = context.packageManager
        return try {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .mapNotNull { appInfo ->
                    try {
                        val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                        if (launchIntent != null) {
                            AppInfo(
                                name = packageManager.getApplicationLabel(appInfo).toString(),
                                packageName = appInfo.packageName,
                                icon = packageManager.getApplicationIcon(appInfo),
                                activityName = launchIntent.component?.className ?: ""
                            )
                        } else null
                    } catch (e: Exception) { null }
                }.sortedBy { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun openApp(context: Context, packageName: String) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}