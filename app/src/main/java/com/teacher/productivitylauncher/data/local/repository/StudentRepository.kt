package com.teacher.productivitylauncher.data.local.repository

import com.teacher.productivitylauncher.data.local.dao.StudentDao
import com.teacher.productivitylauncher.data.local.entity.Student
import kotlinx.coroutines.flow.Flow

class StudentRepository(private val studentDao: StudentDao) {

    fun getAllStudents(): Flow<List<Student>> = studentDao.getAllStudents()

    fun getStudentsByClass(className: String): Flow<List<Student>> =
        studentDao.getStudentsByClass(className)

    fun getStudentsWithFeesDue(): Flow<List<Student>> =
        studentDao.getStudentsWithFeesDue()

    suspend fun insertStudent(student: Student) = studentDao.insertStudent(student)

    suspend fun updateStudent(student: Student) = studentDao.updateStudent(student)

    suspend fun deleteStudent(student: Student) = studentDao.deleteStudent(student)

    suspend fun getStudentById(id: Int): Student? = studentDao.getStudentById(id)
}