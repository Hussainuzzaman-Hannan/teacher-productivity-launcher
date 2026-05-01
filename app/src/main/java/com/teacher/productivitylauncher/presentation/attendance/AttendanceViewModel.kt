package com.teacher.productivitylauncher.presentation.attendance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.Attendance
import com.teacher.productivitylauncher.data.local.entity.Student
import com.teacher.productivitylauncher.data.local.repository.AttendanceRepository
import com.teacher.productivitylauncher.data.local.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val studentRepository = StudentRepository(
        TeacherDatabase.getDatabase(application).studentDao()
    )
    private val attendanceRepository = AttendanceRepository(
        TeacherDatabase.getDatabase(application).attendanceDao()
    )

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _selectedClass = MutableStateFlow("")
    val selectedClass: StateFlow<String> = _selectedClass.asStateFlow()

    private val _attendanceMap = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val attendanceMap: StateFlow<Map<Int, Boolean>> = _attendanceMap.asStateFlow()

    private val _presentCount = MutableStateFlow(0)
    val presentCount: StateFlow<Int> = _presentCount.asStateFlow()

    private val _absentCount = MutableStateFlow(0)
    val absentCount: StateFlow<Int> = _absentCount.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _savedAttendance = MutableStateFlow(false)
    val savedAttendance: StateFlow<Boolean> = _savedAttendance.asStateFlow()

    init {
        loadAllStudents()
    }

    private fun loadAllStudents() {
        viewModelScope.launch {
            studentRepository.getAllStudents().collect { studentList ->
                _students.value = studentList
                // Initialize attendance map for all students
                val currentMap = mutableMapOf<Int, Boolean>()
                studentList.forEach { student ->
                    currentMap[student.id] = false
                }
                _attendanceMap.value = currentMap
                updateCounts()
            }
        }
    }

    fun filterByClass(className: String) {
        _selectedClass.value = className
        viewModelScope.launch {
            if (className.isEmpty()) {
                studentRepository.getAllStudents().collect { studentList ->
                    _students.value = studentList
                    initializeAttendanceMap(studentList)
                }
            } else {
                studentRepository.getStudentsByClass(className).collect { studentList ->
                    _students.value = studentList
                    initializeAttendanceMap(studentList)
                }
            }
        }
    }

    private fun initializeAttendanceMap(studentList: List<Student>) {
        val currentMap = mutableMapOf<Int, Boolean>()
        studentList.forEach { student ->
            currentMap[student.id] = false
        }
        _attendanceMap.value = currentMap
        updateCounts()
    }

    fun toggleAttendance(studentId: Int) {
        val currentMap = _attendanceMap.value.toMutableMap()
        currentMap[studentId] = !(currentMap[studentId] ?: false)
        _attendanceMap.value = currentMap
        updateCounts()
    }

    private fun updateCounts() {
        val present = _attendanceMap.value.values.count { it }
        _presentCount.value = present
        _absentCount.value = _students.value.size - present
    }

    fun saveAttendance(date: Date) {
        viewModelScope.launch {
            try {
                // Delete existing attendance for this date
                val existingAttendance = attendanceRepository.getAttendanceByDate(date)
                existingAttendance.collect { list ->
                    list.forEach { attendance ->
                        attendanceRepository.deleteAttendance(attendance)
                    }
                }

                // Save new attendance
                _attendanceMap.value.forEach { (studentId, isPresent) ->
                    val attendance = Attendance(
                        studentId = studentId,
                        className = _selectedClass.value.ifEmpty { "All Classes" },
                        date = date,
                        isPresent = isPresent,
                        remarks = if (isPresent) "Present" else "Absent"
                    )
                    attendanceRepository.insertAttendance(attendance)
                }

                _savedAttendance.value = true
                _message.value = "Attendance saved successfully!"

                // Reset saved flag after 2 seconds
                kotlinx.coroutines.delay(2000)
                _savedAttendance.value = false
                _message.value = ""

            } catch (e: Exception) {
                _message.value = "Error saving attendance: ${e.message}"
            }
        }
    }

    fun loadAttendanceForDate(date: Date, className: String) {
        viewModelScope.launch {
            _selectedClass.value = className
            val attendanceList = attendanceRepository.getAttendanceByDate(date)
            attendanceList.collect { list ->
                val attendanceMap = mutableMapOf<Int, Boolean>()
                list.forEach { attendance ->
                    attendanceMap[attendance.studentId] = attendance.isPresent
                }

                // Update only for students in current class
                val currentStudents = if (className.isEmpty()) _students.value
                else _students.value.filter { it.className == className }

                currentStudents.forEach { student ->
                    if (!attendanceMap.containsKey(student.id)) {
                        attendanceMap[student.id] = false
                    }
                }

                _attendanceMap.value = attendanceMap
                updateCounts()
            }
        }
    }

    fun clearMessage() {
        _message.value = ""
    }

    fun getUniqueClasses(): List<String> {
        return _students.value.map { it.className }.distinct()
    }
}