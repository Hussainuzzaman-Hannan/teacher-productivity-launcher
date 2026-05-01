package com.teacher.productivitylauncher.data.local.dao

import androidx.room.*
import com.teacher.productivitylauncher.data.local.entity.Attendance
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceByDate(date: Date): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId")
    fun getAttendanceByStudent(studentId: Int): Flow<List<Attendance>>

    @Query("SELECT COUNT(*) FROM attendance WHERE date = :date AND isPresent = 1")
    suspend fun getPresentCount(date: Date): Int

    @Query("SELECT COUNT(*) FROM attendance WHERE date = :date AND isPresent = 0")
    suspend fun getAbsentCount(date: Date): Int

    @Insert
    suspend fun insertAttendance(attendance: Attendance)

    @Update
    suspend fun updateAttendance(attendance: Attendance)

    @Delete
    suspend fun deleteAttendance(attendance: Attendance)

    @Query("SELECT * FROM attendance WHERE studentId = :studentId AND date = :date")
    suspend fun getAttendanceByStudentAndDate(studentId: Int, date: Date): Attendance?

    @Query("DELETE FROM attendance WHERE date = :date")
    suspend fun deleteAttendanceByDate(date: Date)

    // Get all attendance records (for backup)
    @Query("SELECT * FROM attendance")
    fun getAllAttendance(): Flow<List<Attendance>>
}