package com.teacher.productivitylauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_folders")
data class AppFolder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val folderName: String,
    val packageNames: String, // Comma separated package names
    val createdDate: Long = System.currentTimeMillis()
)