package com.teacher.productivitylauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "class_routine")
data class ClassRoutine(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subjectName: String,
    val className: String,
    val teacherName: String,
    val dayOfWeek: Int,        // 1=Monday, 2=Tuesday, ... 7=Sunday
    val startTime: String,      // "09:00 AM"
    val endTime: String,        // "10:00 AM"
    val roomNumber: String,
    val notificationEnabled: Boolean = true
)