package com.teacher.productivitylauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val className: String,      // Class 5, Class 8 etc
    val batch: String,           // Batch name or section
    val phone: String,
    val guardianName: String,
    val guardianPhone: String,
    val feesDue: Double,
    val joinDate: Date,
    val isActive: Boolean = true
)