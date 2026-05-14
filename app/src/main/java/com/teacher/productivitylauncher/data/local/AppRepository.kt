package com.teacher.productivitylauncher.data.local

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    // 🔥 ক্যাশ মেকানিজম এপ্লিকেশন লেভেলে
    private var cachedAppList: List<AppInfo>? = null
    private var lastCacheTime = 0L
    private val CACHE_DURATION = 60000L // 1 মিনিট ক্যাশ ভালো থাকবে

    fun getInstalledApps(forceRefresh: Boolean = false): Flow<List<AppInfo>> = flow {
        val apps = withContext(Dispatchers.IO) {
            if (forceRefresh || !isCacheValid()) {
                android.util.Log.d("AppRepository", "🔄 Loading fresh app list")
                getAppListFast().also {
                    cachedAppList = it
                    lastCacheTime = System.currentTimeMillis()
                }
            } else {
                android.util.Log.d("AppRepository", "⚡ Using cached app list (${cachedAppList?.size} apps)")
                cachedAppList ?: getAppListFast().also {
                    cachedAppList = it
                    lastCacheTime = System.currentTimeMillis()
                }
            }
        }
        emit(apps)
    }

    private fun isCacheValid(): Boolean {
        return cachedAppList != null &&
                (System.currentTimeMillis() - lastCacheTime) < CACHE_DURATION
    }

    // 🔥 নতুন: একক, দ্রুততম মেথড (শুধু MATCH_ALL ব্যবহার করে)
    private fun getAppListFast(): List<AppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return try {
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                PackageManager.MATCH_ALL or PackageManager.GET_META_DATA
            } else {
                0
            }

            val resolvedActivities = packageManager.queryIntentActivities(intent, flags)

            android.util.Log.d("AppRepository", "✅ Fast method found ${resolvedActivities.size} apps")

            resolvedActivities.mapNotNull { resolveInfo ->
                try {
                    AppInfo(
                        name = resolveInfo.loadLabel(packageManager).toString(),
                        packageName = resolveInfo.activityInfo.packageName,
                        icon = resolveInfo.loadIcon(packageManager),
                        activityName = resolveInfo.activityInfo.name
                    )
                } catch (e: Exception) {
                    android.util.Log.e("AppRepository", "Error loading app: ${e.message}")
                    null
                }
            }.sortedBy { it.name }

        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Fast method failed: ${e.message}, trying fallback")
            getAppListFallback()
        }
    }

    // 🔥 ব্যাকআপ মেথড (যদি উপরের কাজ না করে)
    private fun getAppListFallback(): List<AppInfo> {
        val packageManager = context.packageManager
        return try {
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            packages.mapNotNull { appInfo ->
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
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.name }

        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Fallback method failed: ${e.message}")
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

    // 🔥 ক্যাশ রিসেট করার জন্য (যখন অ্যাপ ইন্সটল/আনইন্সটল হয়)
    fun invalidateCache() {
        cachedAppList = null
        lastCacheTime = 0L
        android.util.Log.d("AppRepository", "Cache invalidated")
    }
}