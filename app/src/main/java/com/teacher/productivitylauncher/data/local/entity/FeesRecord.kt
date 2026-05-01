package com.teacher.productivitylauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "fees_records")
data class FeesRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val studentId: Int,
    val studentName: String,
    val className: String,
    val amount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val paymentDate: Date,
    val paymentMethod: String, // "Cash", "Bank", "Mobile Banking"
    val remarks: String? = null,
    val month: String, // "January 2024" format
    val isPaid: Boolean = false
)

@Entity(tableName = "fees_summary")
data class FeesSummary(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val className: String,
    val totalFees: Double,
    val totalPaid: Double,
    val totalDue: Double,
    val month: String
)