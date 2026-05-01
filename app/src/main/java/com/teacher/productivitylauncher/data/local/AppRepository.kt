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

    fun getInstalledApps(): Flow<List<AppInfo>> = flow {
        val apps = withContext(Dispatchers.IO) {
            getAppList()
        }
        android.util.Log.d("AppRepository", "Found ${apps.size} apps")
        emit(apps)
    }

    private fun getAppList(): List<AppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // Try multiple methods to get apps
        var resolvedActivities: List<ResolveInfo> = emptyList()

        // Method 1: Standard query
        try {
            resolvedActivities = packageManager.queryIntentActivities(intent, 0)
            android.util.Log.d("AppRepository", "Method 1: Found ${resolvedActivities.size} apps")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Method 1 failed: ${e.message}")
        }

        // Method 2: For Android 11+ with MATCH_ALL flag
        if (resolvedActivities.isEmpty() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            try {
                resolvedActivities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                android.util.Log.d("AppRepository", "Method 2: Found ${resolvedActivities.size} apps")
            } catch (e: Exception) {
                android.util.Log.e("AppRepository", "Method 2 failed: ${e.message}")
            }
        }

        // Method 3: If still empty, try with GET_ACTIVITIES
        if (resolvedActivities.isEmpty()) {
            try {
                val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                android.util.Log.d("AppRepository", "Method 3: Found ${packages.size} packages")

                resolvedActivities = packages.mapNotNull { appInfo ->
                    val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        val resolveInfo = packageManager.resolveActivity(launchIntent, 0)
                        resolveInfo?.let { ri ->
                            ResolveInfo().apply {
                                activityInfo = ri.activityInfo
                            }
                        }
                    } else null
                }
            } catch (e: Exception) {
                android.util.Log.e("AppRepository", "Method 3 failed: ${e.message}")
            }
        }

        return resolvedActivities.mapNotNull { resolveInfo ->
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
    }

    fun openApp(context: Context, packageName: String) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.let {
                context.startActivity(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}