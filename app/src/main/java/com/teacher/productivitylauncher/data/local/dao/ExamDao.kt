package com.teacher.productivitylauncher.data.local.dao

import androidx.room.*
import com.teacher.productivitylauncher.data.local.entity.Exam
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY examDate ASC")
    fun getAllExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE examDate >= :currentDate ORDER BY examDate ASC LIMIT 1")
    suspend fun getNextExam(currentDate: Date): Exam?

    @Insert
    suspend fun insertExam(exam: Exam)

    @Update
    suspend fun updateExam(exam: Exam)

    @Delete
    suspend fun deleteExam(exam: Exam)

    // Get all exams (for backup)
    @Query("SELECT * FROM exams")
    fun getAllExamsForBackup(): Flow<List<Exam>>
}