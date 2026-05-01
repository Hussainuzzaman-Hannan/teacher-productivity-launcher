package com.teacher.productivitylauncher.data.local.dao

import androidx.room.*
import com.teacher.productivitylauncher.data.local.entity.RenamedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface RenamedAppDao {
    @Query("SELECT * FROM renamed_apps")
    fun getAllRenamed(): Flow<List<RenamedApp>>

    @Query("SELECT * FROM renamed_apps WHERE packageName = :packageName")
    suspend fun getRenamedApp(packageName: String): RenamedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun renameApp(renamedApp: RenamedApp)

    @Delete
    suspend fun deleteRenamedApp(renamedApp: RenamedApp)

    @Query("DELETE FROM renamed_apps WHERE packageName = :packageName")
    suspend fun resetAppName(packageName: String)
}