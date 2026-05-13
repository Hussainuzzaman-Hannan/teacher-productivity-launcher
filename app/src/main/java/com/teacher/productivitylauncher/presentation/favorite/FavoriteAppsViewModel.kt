package com.teacher.productivitylauncher.presentation.favorite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
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
        val currentList = _favoriteApps.value
        if (currentList.size >= MAX_FAVORITES) {
            _maxLimitReached.value = true
            return false
        }
        val favorite = FavoriteApp(
            packageName = packageName,
            appName = appName,
            sortOrder = currentList.size
        )
        repository.addFavorite(favorite)
        return true
    }

    suspend fun removeFromFavorites(packageName: String) {
        repository.removeFavorite(packageName)
    }

    suspend fun isFavorite(packageName: String): Boolean {
        return repository.isFavorite(packageName)
    }

    // Drag & drop এ নতুন order save করে
    fun reorder(newList: List<FavoriteApp>) {
        _favoriteApps.value = newList
        viewModelScope.launch {
            repository.updateOrder(newList)
        }
    }
}

class FavoriteAppsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoriteAppsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavoriteAppsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}