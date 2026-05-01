package com.teacher.productivitylauncher.data.local.dao

import androidx.room.*
import com.teacher.productivitylauncher.data.local.entity.FavoriteApp
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteAppDao {
    @Query("SELECT * FROM favorite_apps ORDER BY addedDate DESC")
    fun getAllFavorites(): Flow<List<FavoriteApp>>

    @Query("SELECT * FROM favorite_apps WHERE packageName = :packageName")
    suspend fun isFavorite(packageName: String): FavoriteApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favoriteApp: FavoriteApp)

    @Delete
    suspend fun removeFavorite(favoriteApp: FavoriteApp)

    @Query("DELETE FROM favorite_apps WHERE packageName = :packageName")
    suspend fun removeFavoriteByPackage(packageName: String)
}