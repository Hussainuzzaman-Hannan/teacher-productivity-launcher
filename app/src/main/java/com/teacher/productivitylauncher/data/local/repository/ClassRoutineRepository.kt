package com.teacher.productivitylauncher.data.local.repository

import com.teacher.productivitylauncher.data.local.dao.ClassRoutineDao
import com.teacher.productivitylauncher.data.local.entity.ClassRoutine
import kotlinx.coroutines.flow.Flow

class ClassRoutineRepository(private val classRoutineDao: ClassRoutineDao) {

    fun getRoutineByDay(day: Int): Flow<List<ClassRoutine>> =
        classRoutineDao.getRoutineByDay(day)

    fun getRoutineByClass(className: String): Flow<List<ClassRoutine>> =
        classRoutineDao.getRoutineByClass(className)

    fun getRoutineByDayAndClass(day: Int, className: String): Flow<List<ClassRoutine>> =
        classRoutineDao.getRoutineByDayAndClass(day, className)

    suspend fun insertRoutine(routine: ClassRoutine) {
        classRoutineDao.insertRoutine(routine)
    }

    suspend fun updateRoutine(routine: ClassRoutine) {
        classRoutineDao.updateRoutine(routine)
    }

    suspend fun deleteRoutine(routine: ClassRoutine) {
        classRoutineDao.deleteRoutine(routine)
    }

    suspend fun getRoutineById(id: Int): ClassRoutine? =
        classRoutineDao.getRoutineById(id)

    suspend fun getDistinctClasses(): List<String> =
        classRoutineDao.getDistinctClasses()
}