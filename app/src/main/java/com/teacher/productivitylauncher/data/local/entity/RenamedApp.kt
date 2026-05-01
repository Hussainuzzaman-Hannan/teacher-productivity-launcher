package com.teacher.productivitylauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "renamed_apps")
data class RenamedApp(
    @PrimaryKey
    val packageName: String,
    val newName: String,
    val originalName: String
)