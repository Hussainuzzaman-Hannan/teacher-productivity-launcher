package com.teacher.productivitylauncher.data.local.repository

import com.teacher.productivitylauncher.data.local.dao.AttendanceDao
import com.teacher.productivitylauncher.data.local.entity.Attendance
import java.util.Date

class AttendanceRepository(private val attendanceDao: AttendanceDao) {

    fun getAttendanceByDate(date: Date) = attendanceDao.getAttendanceByDate(date)

    fun getAttendanceByStudent(studentId: Int) = attendanceDao.getAttendanceByStudent(studentId)

    suspend fun insertAttendance(attendance: Attendance) =
        attendanceDao.insertAttendance(attendance)

    suspend fun updateAttendance(attendance: Attendance) =
        attendanceDao.updateAttendance(attendance)

    suspend fun deleteAttendance(attendance: Attendance) =
        attendanceDao.deleteAttendance(attendance)  // Add this line

    suspend fun getAttendanceByStudentAndDate(studentId: Int, date: Date) =
        attendanceDao.getAttendanceByStudentAndDate(studentId, date)

    suspend fun getPresentCount(date: Date) = attendanceDao.getPresentCount(date)

    suspend fun getAbsentCount(date: Date) = attendanceDao.getAbsentCount(date)
}