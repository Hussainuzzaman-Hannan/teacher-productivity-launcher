package com.teacher.productivitylauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_apps")
data class HiddenApp(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val hiddenDate: Long = System.currentTimeMillis()
)