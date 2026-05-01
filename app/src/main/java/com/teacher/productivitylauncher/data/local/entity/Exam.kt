package com.teacher.productivitylauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val examName: String,
    val className: String,
    val examDate: Date,
    val description: String? = null
)