package com.teacher.productivitylauncher.data.local.dao

import androidx.room.*
import com.teacher.productivitylauncher.data.local.entity.BlockedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps")
    fun getAllBlocked(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName")
    suspend fun isBlocked(packageName: String): BlockedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockApp(blockedApp: BlockedApp)

    @Delete
    suspend fun unblockApp(blockedApp: BlockedApp)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun unblockAppByPackage(packageName: String)
}