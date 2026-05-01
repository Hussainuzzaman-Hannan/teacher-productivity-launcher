package com.teacher.productivitylauncher.data.local.dao

import androidx.room.*
import com.teacher.productivitylauncher.data.local.entity.FeesRecord
import com.teacher.productivitylauncher.data.local.entity.FeesSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface FeesDao {

    // Fees Record Queries
    @Query("SELECT * FROM fees_records ORDER BY paymentDate DESC")
    fun getAllFeesRecords(): Flow<List<FeesRecord>>

    @Query("SELECT * FROM fees_records WHERE studentId = :studentId ORDER BY paymentDate DESC")
    fun getFeesByStudent(studentId: Int): Flow<List<FeesRecord>>

    @Query("SELECT * FROM fees_records WHERE className = :className ORDER BY paymentDate DESC")
    fun getFeesByClass(className: String): Flow<List<FeesRecord>>

    @Query("SELECT * FROM fees_records WHERE month = :month")
    fun getFeesByMonth(month: String): Flow<List<FeesRecord>>

    @Query("SELECT * FROM fees_records WHERE isPaid = 0 ORDER BY dueAmount DESC")
    fun getUnpaidFees(): Flow<List<FeesRecord>>

    @Insert
    suspend fun insertFeesRecord(record: FeesRecord)

    @Update
    suspend fun updateFeesRecord(record: FeesRecord)

    @Delete
    suspend fun deleteFeesRecord(record: FeesRecord)

    // Fees Summary Queries
    @Query("SELECT * FROM fees_summary WHERE month = :month")
    suspend fun getSummaryByMonth(month: String): FeesSummary?

    @Query("SELECT * FROM fees_summary ORDER BY month DESC")
    fun getAllSummaries(): Flow<List<FeesSummary>>

    @Insert
    suspend fun insertSummary(summary: FeesSummary)

    @Update
    suspend fun updateSummary(summary: FeesSummary)

    @Query("SELECT SUM(paidAmount) FROM fees_records WHERE month = :month")
    suspend fun getTotalPaidByMonth(month: String): Double?

    @Query("SELECT SUM(dueAmount) FROM fees_records WHERE month = :month")
    suspend fun getTotalDueByMonth(month: String): Double?

    @Query("SELECT COUNT(*) FROM fees_records WHERE isPaid = 0 AND month = :month")
    suspend fun getUnpaidCountByMonth(month: String): Int
}