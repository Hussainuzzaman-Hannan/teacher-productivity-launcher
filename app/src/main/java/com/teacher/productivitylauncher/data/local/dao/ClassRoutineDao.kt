package com.teacher.productivitylauncher.data.local.dao

import androidx.room.*
import com.teacher.productivitylauncher.data.local.entity.ClassRoutine
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassRoutineDao {

    @Query("SELECT * FROM class_routine WHERE dayOfWeek = :day ORDER BY startTime ASC")
    fun getRoutineByDay(day: Int): Flow<List<ClassRoutine>>

    @Query("SELECT * FROM class_routine WHERE className = :className ORDER BY dayOfWeek, startTime ASC")
    fun getRoutineByClass(className: String): Flow<List<ClassRoutine>>

    @Query("SELECT * FROM class_routine WHERE dayOfWeek = :day AND className = :className ORDER BY startTime ASC")
    fun getRoutineByDayAndClass(day: Int, className: String): Flow<List<ClassRoutine>>

    @Insert
    suspend fun insertRoutine(routine: ClassRoutine): Long

    @Update
    suspend fun updateRoutine(routine: ClassRoutine)

    @Delete
    suspend fun deleteRoutine(routine: ClassRoutine)

    @Query("SELECT * FROM class_routine WHERE id = :id")
    suspend fun getRoutineById(id: Int): ClassRoutine?

    @Query("SELECT DISTINCT className FROM class_routine")
    suspend fun getDistinctClasses(): List<String>

    // 🔥 সব ক্লাস রুটিন পাওয়ার জন্য নতুন ফাংশন (রিমাইন্ডারের জন্য প্রয়োজন)
    @Query("SELECT * FROM class_routine")
    fun getAllRoutinesFlow(): Flow<List<ClassRoutine>>

    // 🔥 Suspend version for reminder manager
    @Query("SELECT * FROM class_routine")
    suspend fun getAllRoutines(): List<ClassRoutine>
}