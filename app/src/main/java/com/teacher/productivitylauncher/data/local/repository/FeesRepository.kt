package com.teacher.productivitylauncher.data.local.repository

import com.teacher.productivitylauncher.data.local.dao.FeesDao
import com.teacher.productivitylauncher.data.local.entity.FeesRecord
import com.teacher.productivitylauncher.data.local.entity.FeesSummary
import kotlinx.coroutines.flow.Flow

class FeesRepository(private val feesDao: FeesDao) {

    // Fees Record
    fun getAllFeesRecords(): Flow<List<FeesRecord>> = feesDao.getAllFeesRecords()

    fun getFeesByStudent(studentId: Int): Flow<List<FeesRecord>> = feesDao.getFeesByStudent(studentId)

    fun getFeesByClass(className: String): Flow<List<FeesRecord>> = feesDao.getFeesByClass(className)

    fun getFeesByMonth(month: String): Flow<List<FeesRecord>> = feesDao.getFeesByMonth(month)

    fun getUnpaidFees(): Flow<List<FeesRecord>> = feesDao.getUnpaidFees()

    suspend fun insertFeesRecord(record: FeesRecord) = feesDao.insertFeesRecord(record)

    suspend fun updateFeesRecord(record: FeesRecord) = feesDao.updateFeesRecord(record)

    suspend fun deleteFeesRecord(record: FeesRecord) = feesDao.deleteFeesRecord(record)

    // Summary
    suspend fun getSummaryByMonth(month: String): FeesSummary? = feesDao.getSummaryByMonth(month)

    fun getAllSummaries(): Flow<List<FeesSummary>> = feesDao.getAllSummaries()

    suspend fun insertSummary(summary: FeesSummary) = feesDao.insertSummary(summary)

    suspend fun updateSummary(summary: FeesSummary) = feesDao.updateSummary(summary)

    suspend fun getTotalPaidByMonth(month: String): Double? = feesDao.getTotalPaidByMonth(month)

    suspend fun getTotalDueByMonth(month: String): Double? = feesDao.getTotalDueByMonth(month)

    suspend fun getUnpaidCountByMonth(month: String): Int = feesDao.getUnpaidCountByMonth(month)
}