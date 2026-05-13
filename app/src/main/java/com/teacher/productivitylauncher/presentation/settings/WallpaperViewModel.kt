package com.teacher.productivitylauncher.presentation.settings

import android.app.Application
import android.app.WallpaperManager
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

private val Context.wallpaperDataStore by preferencesDataStore(name = "wallpaper_settings")

class WallpaperViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val wallpaperManager = WallpaperManager.getInstance(context)

    private val HOME_WALLPAPER_URI = stringPreferencesKey("home_wallpaper_uri")
    private val LOCK_WALLPAPER_URI = stringPreferencesKey("lock_wallpaper_uri")
    private val BLUR_AMOUNT = floatPreferencesKey("blur_amount")
    private val DARK_OVERLAY = floatPreferencesKey("dark_overlay")

    private val _homeWallpaperUri = MutableStateFlow<String?>(null)
    val homeWallpaperUri: StateFlow<String?> = _homeWallpaperUri.asStateFlow()

    private val _lockWallpaperUri = MutableStateFlow<String?>(null)
    val lockWallpaperUri: StateFlow<String?> = _lockWallpaperUri.asStateFlow()

    private val _blurAmount = MutableStateFlow(0f)
    val blurAmount: StateFlow<Float> = _blurAmount.asStateFlow()

    private val _darkOverlay = MutableStateFlow(0.3f)
    val darkOverlay: StateFlow<Float> = _darkOverlay.asStateFlow()

    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            context.wallpaperDataStore.data.collect { prefs ->
                _homeWallpaperUri.value = prefs[HOME_WALLPAPER_URI]
                _lockWallpaperUri.value = prefs[LOCK_WALLPAPER_URI]
                _blurAmount.value = prefs[BLUR_AMOUNT] ?: 0f
                _darkOverlay.value = prefs[DARK_OVERLAY] ?: 0.3f
            }
        }
    }

    // 🔥 Save Uri to local file and apply from file (解决 permission 问题)
    private suspend fun copyUriToLocalFile(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val tempFile = File(context.cacheDir, "wallpaper_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun setHomeWallpaper(uri: Uri, onComplete: (Boolean) -> Unit = {}) {
        if (_isApplying.value) {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isApplying.value = true
            try {
                // First copy to local file
                val localFile = copyUriToLocalFile(uri)
                if (localFile != null) {
                    val bitmap = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeFile(localFile.absolutePath)
                    }

                    if (bitmap != null) {
                        withContext(Dispatchers.IO) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                            } else {
                                wallpaperManager.setBitmap(bitmap)
                            }
                        }

                        context.wallpaperDataStore.edit { prefs ->
                            prefs[HOME_WALLPAPER_URI] = uri.toString()
                        }
                        _homeWallpaperUri.value = uri.toString()

                        // Clean up temp file
                        localFile.delete()
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                _isApplying.value = false
            }
        }
    }

    fun setLockWallpaper(uri: Uri, onComplete: (Boolean) -> Unit = {}) {
        if (_isApplying.value) {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isApplying.value = true
            try {
                // First copy to local file
                val localFile = copyUriToLocalFile(uri)
                if (localFile != null) {
                    val bitmap = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeFile(localFile.absolutePath)
                    }

                    if (bitmap != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        withContext(Dispatchers.IO) {
                            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                        }

                        context.wallpaperDataStore.edit { prefs ->
                            prefs[LOCK_WALLPAPER_URI] = uri.toString()
                        }
                        _lockWallpaperUri.value = uri.toString()

                        // Clean up temp file
                        localFile.delete()
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                _isApplying.value = false
            }
        }
    }

    fun removeHomeWallpaper() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.clear(WallpaperManager.FLAG_SYSTEM)
                    }
                }

                context.wallpaperDataStore.edit { prefs ->
                    prefs.remove(HOME_WALLPAPER_URI)
                }
                _homeWallpaperUri.value = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeLockWallpaper() {
        viewModelScope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    withContext(Dispatchers.IO) {
                        wallpaperManager.clear(WallpaperManager.FLAG_LOCK)
                    }
                }

                context.wallpaperDataStore.edit { prefs ->
                    prefs.remove(LOCK_WALLPAPER_URI)
                }
                _lockWallpaperUri.value = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setBothWallpapers(uri: Uri, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isApplying.value = true
            try {
                val localFile = copyUriToLocalFile(uri)
                if (localFile != null) {
                    val bitmap = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeFile(localFile.absolutePath)
                    }

                    if (bitmap != null) {
                        withContext(Dispatchers.IO) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                            } else {
                                wallpaperManager.setBitmap(bitmap)
                            }
                        }

                        context.wallpaperDataStore.edit { prefs ->
                            prefs[HOME_WALLPAPER_URI] = uri.toString()
                            prefs[LOCK_WALLPAPER_URI] = uri.toString()
                        }
                        _homeWallpaperUri.value = uri.toString()
                        _lockWallpaperUri.value = uri.toString()

                        localFile.delete()
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                _isApplying.value = false
            }
        }
    }

    fun setBlurAmount(amount: Float) {
        viewModelScope.launch {
            context.wallpaperDataStore.edit { prefs ->
                prefs[BLUR_AMOUNT] = amount
            }
            _blurAmount.value = amount
        }
    }

    fun setDarkOverlay(amount: Float) {
        viewModelScope.launch {
            context.wallpaperDataStore.edit { prefs ->
                prefs[DARK_OVERLAY] = amount
            }
            _darkOverlay.value = amount
        }
    }
}