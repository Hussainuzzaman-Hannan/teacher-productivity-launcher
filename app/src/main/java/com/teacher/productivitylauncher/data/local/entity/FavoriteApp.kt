package com.teacher.productivitylauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_apps")
data class FavoriteApp(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val addedDate: Long = System.currentTimeMillis()
)