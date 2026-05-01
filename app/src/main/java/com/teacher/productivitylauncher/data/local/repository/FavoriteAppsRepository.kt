package com.teacher.productivitylauncher.data.local.repository

import com.teacher.productivitylauncher.data.local.dao.FavoriteAppDao
import com.teacher.productivitylauncher.data.local.entity.FavoriteApp
import kotlinx.coroutines.flow.Flow

class FavoriteAppsRepository(private val favoriteAppDao: FavoriteAppDao) {

    fun getAllFavorites(): Flow<List<FavoriteApp>> = favoriteAppDao.getAllFavorites()

    suspend fun isFavorite(packageName: String): Boolean {
        return favoriteAppDao.isFavorite(packageName) != null
    }

    suspend fun addFavorite(favoriteApp: FavoriteApp) {
        favoriteAppDao.addFavorite(favoriteApp)
    }

    suspend fun removeFavorite(packageName: String) {
        favoriteAppDao.removeFavoriteByPackage(packageName)
    }
}