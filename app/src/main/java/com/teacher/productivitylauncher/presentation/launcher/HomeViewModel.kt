package com.teacher.productivitylauncher.presentation.launcher

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.ClassRoutine
import com.teacher.productivitylauncher.data.local.repository.AttendanceRepository
import com.teacher.productivitylauncher.data.local.repository.ClassRoutineRepository
import com.teacher.productivitylauncher.data.local.repository.FeesRepository
import com.teacher.productivitylauncher.data.local.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ClassStatus data class
data class ClassStatus(
    val subjectName: String,
    val startTime: String,
    val endTime: String,
    val isCompleted: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val classRoutineRepository = ClassRoutineRepository(
        TeacherDatabase.getDatabase(application).classRoutineDao()
    )
    private val attendanceRepository = AttendanceRepository(
        TeacherDatabase.getDatabase(application).attendanceDao()
    )
    private val studentRepository = StudentRepository(
        TeacherDatabase.getDatabase(application).studentDao()
    )
    private val feesRepository = FeesRepository(
        TeacherDatabase.getDatabase(application).feesDao()
    )

    private val _todayClasses = MutableStateFlow<List<ClassRoutine>>(emptyList())
    val todayClasses: StateFlow<List<ClassRoutine>> = _todayClasses.asStateFlow()

    private val _todayClassesCount = MutableStateFlow(0)
    val todayClassesCount: StateFlow<Int> = _todayClassesCount.asStateFlow()

    private val _presentCount = MutableStateFlow(0)
    val presentCount: StateFlow<Int> = _presentCount.asStateFlow()

    private val _absentCount = MutableStateFlow(0)
    val absentCount: StateFlow<Int> = _absentCount.asStateFlow()

    private val _totalStudents = MutableStateFlow(0)
    val totalStudents: StateFlow<Int> = _totalStudents.asStateFlow()

    private val _feesDueCount = MutableStateFlow(0)
    val feesDueCount: StateFlow<Int> = _feesDueCount.asStateFlow()

    private val _feesDueAmount = MutableStateFlow(0.0)
    val feesDueAmount: StateFlow<Double> = _feesDueAmount.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Class tracking
    private val _classStatuses = MutableStateFlow<List<ClassStatus>>(emptyList())
    val classStatuses: StateFlow<List<ClassStatus>> = _classStatuses.asStateFlow()

    private val _completedClassesCount = MutableStateFlow(0)
    val completedClassesCount: StateFlow<Int> = _completedClassesCount.asStateFlow()

    // Reminder manager
    private var reminderManager: ClassReminderManager? = null

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            _isLoading.value = true

            loadTodayClasses()
            loadTodayAttendanceSummary()
            loadFeesDueSummary()

            _isLoading.value = false
        }
    }

    private fun loadTodayClasses() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        // Calendar: SUNDAY=1, MONDAY=2, TUESDAY=3, WEDNESDAY=4,
        //           THURSDAY=5, FRIDAY=6, SATURDAY=7
        // আমাদের DB: MONDAY=1, TUESDAY=2, WEDNESDAY=3, THURSDAY=4,
        //            FRIDAY=5, SATURDAY=6, SUNDAY=7
        val dayOfWeek = when (today) {
            Calendar.SUNDAY -> 7
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 1
        }

        viewModelScope.launch {
            classRoutineRepository.getRoutineByDay(dayOfWeek).collect { routines ->
                _todayClasses.value = routines
                _todayClassesCount.value = routines.size
                updateClassStatuses(Calendar.getInstance().time)
            }
        }
    }

    private fun loadTodayAttendanceSummary() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayDate = calendar.time

        viewModelScope.launch {
            studentRepository.getAllStudents().collect { students ->
                _totalStudents.value = students.size
                val present = _presentCount.value
                _absentCount.value = students.size - present
            }

            val present = attendanceRepository.getPresentCount(todayDate)
            _presentCount.value = present
            _absentCount.value = _totalStudents.value - present
        }
    }

    private fun loadFeesDueSummary() {
        viewModelScope.launch {
            studentRepository.getStudentsWithFeesDue().collect { studentsWithDue ->
                val count = studentsWithDue.size
                val totalDue = studentsWithDue.sumOf { it.feesDue }

                _feesDueCount.value = count
                _feesDueAmount.value = totalDue
            }
        }
    }

    fun refreshData() {
        loadAllData()
    }

    // 🔥 Initialize reminders
    fun initReminders(context: Context) {
        reminderManager = ClassReminderManager(context)
        viewModelScope.launch {
            reminderManager?.scheduleAllReminders()
        }
    }

    // 🔥 AM/PM সাপোর্ট সহ সঠিক সময় তুলনা করার ফাংশন
    private fun isClassCompleted(endTime: String, currentTime: Date): Boolean {
        return try {
            val currentCalendar = Calendar.getInstance().apply {
                time = currentTime
            }

            // Parse time with AM/PM format (e.g., "12:20 AM", "02:30 PM")
            val timeStr = endTime.trim()

            // Check if time has AM/PM
            val hasAmPm = timeStr.contains("AM", ignoreCase = true) || timeStr.contains(
                "PM",
                ignoreCase = true
            )

            val hour: Int
            val minute: Int

            if (hasAmPm) {
                // Parse "12:20 AM" or "2:30 PM" format
                val amPmPart = if (timeStr.contains("AM", ignoreCase = true)) "AM" else "PM"
                val timeWithoutAmPm = timeStr.replace(amPmPart, "", ignoreCase = true).trim()
                val timeParts = timeWithoutAmPm.split(":")

                var rawHour = timeParts[0].toIntOrNull() ?: return false
                val rawMinute = if (timeParts.size > 1) timeParts[1].toIntOrNull() ?: 0 else 0

                minute = rawMinute

                // Convert to 24-hour format
                hour = when {
                    amPmPart.equals("AM", ignoreCase = true) -> {
                        if (rawHour == 12) 0 else rawHour
                    }

                    else -> { // PM
                        if (rawHour == 12) 12 else rawHour + 12
                    }
                }
            } else {
                // Parse "14:30" format
                val timeParts = timeStr.split(":")
                if (timeParts.size < 2) return false
                hour = timeParts[0].toIntOrNull() ?: return false
                minute = timeParts[1].toIntOrNull() ?: 0
            }

            val endCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // For debugging
            android.util.Log.d(
                "HomeViewModel",
                "Comparing: Current=${currentCalendar.get(Calendar.HOUR_OF_DAY)}:${
                    currentCalendar.get(Calendar.MINUTE)
                }, End=$hour:$minute, Result=${currentCalendar.after(endCalendar)}"
            )

            currentCalendar.after(endCalendar)
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error parsing time: $endTime", e)
            false
        }
    }

    fun updateClassStatuses(currentTime: Date) {
        val currentClasses = _todayClasses.value
        val updatedStatuses = currentClasses.map { routine ->
            val isCompleted = isClassCompleted(routine.endTime, currentTime)
            android.util.Log.d(
                "HomeViewModel",
                "${routine.subjectName}: ${routine.endTime} - Completed: $isCompleted (Current time: ${
                    SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                    ).format(currentTime)
                })"
            )
            ClassStatus(
                subjectName = routine.subjectName,
                startTime = routine.startTime,
                endTime = routine.endTime,
                isCompleted = isCompleted
            )
        }
        _classStatuses.value = updatedStatuses
        _completedClassesCount.value = updatedStatuses.count { it.isCompleted }
    }
}