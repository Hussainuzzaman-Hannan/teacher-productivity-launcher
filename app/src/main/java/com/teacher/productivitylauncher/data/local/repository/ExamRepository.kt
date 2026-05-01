package com.teacher.productivitylauncher.data.local.repository

import com.teacher.productivitylauncher.data.local.dao.ExamDao
import com.teacher.productivitylauncher.data.local.entity.Exam
import kotlinx.coroutines.flow.Flow
import java.util.Date

class ExamRepository(private val examDao: ExamDao) {

    fun getAllExams(): Flow<List<Exam>> = examDao.getAllExams()

    suspend fun getNextExam(currentDate: Date): Exam? = examDao.getNextExam(currentDate)

    suspend fun insertExam(exam: Exam) = examDao.insertExam(exam)

    suspend fun updateExam(exam: Exam) = examDao.updateExam(exam)

    suspend fun deleteExam(exam: Exam) = examDao.deleteExam(exam)
}