package com.teacher.productivitylauncher.data.local.dao

import androidx.room.*
import com.teacher.productivitylauncher.data.local.entity.HiddenApp
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenAppDao {
    @Query("SELECT * FROM hidden_apps")
    fun getAllHidden(): Flow<List<HiddenApp>>

    @Query("SELECT * FROM hidden_apps WHERE packageName = :packageName")
    suspend fun isHidden(packageName: String): HiddenApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideApp(hiddenApp: HiddenApp)

    @Delete
    suspend fun unhideApp(hiddenApp: HiddenApp)

    @Query("DELETE FROM hidden_apps WHERE packageName = :packageName")
    suspend fun unhideAppByPackage(packageName: String)
}