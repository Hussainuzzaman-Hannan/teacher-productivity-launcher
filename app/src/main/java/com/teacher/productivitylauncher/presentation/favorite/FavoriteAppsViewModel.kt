package com.teacher.productivitylauncher.presentation.favorite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.FavoriteApp
import com.teacher.productivitylauncher.data.local.repository.FavoriteAppsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoriteAppsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FavoriteAppsRepository(
        TeacherDatabase.getDatabase(application).favoriteAppDao()
    )

    private val _favoriteApps = MutableStateFlow<List<FavoriteApp>>(emptyList())
    val favoriteApps: StateFlow<List<FavoriteApp>> = _favoriteApps.asStateFlow()

    private val _maxLimitReached = MutableStateFlow(false)
    val maxLimitReached: StateFlow<Boolean> = _maxLimitReached.asStateFlow()

    val MAX_FAVORITES = 5

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            repository.getAllFavorites().collect { apps ->
                _favoriteApps.value = apps
                _maxLimitReached.value = apps.size >= MAX_FAVORITES
            }
        }
    }

    fun refreshFavorites() {
        loadFavorites()
    }

    suspend fun addToFavorites(packageName: String, appName: String): Boolean {
        val currentSize = _favoriteApps.value.size
        if (currentSize >= MAX_FAVORITES) {
            _maxLimitReached.value = true
            return false
        }

        val favorite = FavoriteApp(
            packageName = packageName,
            appName = appName
        )
        repository.addFavorite(favorite)
        loadFavorites()
        return true
    }

    suspend fun removeFromFavorites(packageName: String) {
        repository.removeFavorite(packageName)
        loadFavorites()
    }

    suspend fun isFavorite(packageName: String): Boolean {
        return repository.isFavorite(packageName)
    }
}