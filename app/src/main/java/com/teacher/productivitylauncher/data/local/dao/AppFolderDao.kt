package com.teacher.productivitylauncher.data.local.dao

import androidx.room.*
import com.teacher.productivitylauncher.data.local.entity.AppFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface AppFolderDao {
    @Query("SELECT * FROM app_folders")
    fun getAllFolders(): Flow<List<AppFolder>>

    @Insert
    suspend fun createFolder(folder: AppFolder)

    @Update
    suspend fun updateFolder(folder: AppFolder)

    @Delete
    suspend fun deleteFolder(folder: AppFolder)

    @Query("SELECT * FROM app_folders WHERE folderName = :folderName")
    suspend fun getFolderByName(folderName: String): AppFolder?
}