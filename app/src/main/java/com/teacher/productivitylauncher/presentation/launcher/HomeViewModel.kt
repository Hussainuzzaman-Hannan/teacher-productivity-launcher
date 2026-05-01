package com.teacher.productivitylauncher.presentation.launcher

import android.app.Application
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
import java.util.*

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
        val dayOfWeek = when (today) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        viewModelScope.launch {
            classRoutineRepository.getRoutineByDay(dayOfWeek).collect { routines ->
                _todayClasses.value = routines
                _todayClassesCount.value = routines.size
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
}