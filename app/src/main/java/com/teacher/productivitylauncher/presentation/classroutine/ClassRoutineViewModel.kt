package com.teacher.productivitylauncher.presentation.classroutine

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.productivitylauncher.data.local.SettingsDataStore
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.ClassRoutine
import com.teacher.productivitylauncher.data.local.repository.ClassRoutineRepository
import com.teacher.productivitylauncher.presentation.MainActivity
import com.teacher.productivitylauncher.presentation.utils.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class ClassRoutineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ClassRoutineRepository(
        TeacherDatabase.getDatabase(application).classRoutineDao()
    )
    private val settingsDataStore = SettingsDataStore(application)
    private var areNotificationsEnabled = true

    private val _routines = MutableStateFlow<List<ClassRoutine>>(emptyList())
    val routines: StateFlow<List<ClassRoutine>> = _routines.asStateFlow()

    private val _todayRoutines = MutableStateFlow<List<ClassRoutine>>(emptyList())
    val todayRoutines: StateFlow<List<ClassRoutine>> = _todayRoutines.asStateFlow()

    private val _selectedClass = MutableStateFlow("")
    val selectedClass: StateFlow<String> = _selectedClass.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _availableClasses = MutableStateFlow<List<String>>(emptyList())
    val availableClasses: StateFlow<List<String>> = _availableClasses.asStateFlow()

    init {
        loadAvailableClasses()
        loadTodayRoutines()
        loadNotificationPreference()
    }

    private fun loadNotificationPreference() {
        viewModelScope.launch {
            settingsDataStore.areNotificationsEnabled.collect { enabled ->
                areNotificationsEnabled = enabled
            }
        }
    }

    private fun loadAvailableClasses() {
        viewModelScope.launch {
            val classes = repository.getDistinctClasses()
            _availableClasses.value = classes
        }
    }

    fun loadRoutinesByClass(className: String) {
        _selectedClass.value = className
        viewModelScope.launch {
            repository.getRoutineByClass(className).collect { routineList ->
                _routines.value = routineList
            }
        }
    }

    fun loadTodayRoutines() {
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
            if (_selectedClass.value.isEmpty()) {
                repository.getRoutineByDay(dayOfWeek).collect { routineList ->
                    _todayRoutines.value = routineList
                }
            } else {
                repository.getRoutineByDayAndClass(dayOfWeek, _selectedClass.value).collect { routineList ->
                    _todayRoutines.value = routineList
                }
            }
        }
    }

    fun addRoutine(subjectName: String, className: String, teacherName: String,
                   dayOfWeek: Int, startTime: String, endTime: String, roomNumber: String) {
        viewModelScope.launch {
            val routine = ClassRoutine(
                subjectName = subjectName,
                className = className,
                teacherName = teacherName,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                roomNumber = roomNumber,
                notificationEnabled = true
            )
            repository.insertRoutine(routine)
            _message.value = "Class added successfully"
            loadAvailableClasses()
            loadTodayRoutines()
            clearMessageAfterDelay()

            // Schedule notification for this class
            scheduleClassNotification(routine)
        }
    }

    fun updateRoutine(routine: ClassRoutine) {
        viewModelScope.launch {
            repository.updateRoutine(routine)
            _message.value = "Class updated successfully"
            loadTodayRoutines()
            clearMessageAfterDelay()
        }
    }

    fun deleteRoutine(routine: ClassRoutine) {
        viewModelScope.launch {
            repository.deleteRoutine(routine)
            _message.value = "Class deleted successfully"
            loadAvailableClasses()
            if (_selectedClass.value == routine.className) {
                loadRoutinesByClass(_selectedClass.value)
            }
            loadTodayRoutines()
            clearMessageAfterDelay()
        }
    }

    fun toggleNotification(routine: ClassRoutine) {
        viewModelScope.launch {
            val updated = routine.copy(notificationEnabled = !routine.notificationEnabled)
            repository.updateRoutine(updated)
            _message.value = if (updated.notificationEnabled) "Notifications enabled" else "Notifications disabled"
            clearMessageAfterDelay()
        }
    }

    private fun scheduleClassNotification(routine: ClassRoutine) {
        if (!routine.notificationEnabled || !areNotificationsEnabled) return

        val context = getApplication<Application>()
        val notificationHelper = NotificationHelper(context)
        notificationHelper.createNotificationChannels()
        notificationHelper.showClassNotification(
            routine.subjectName,
            routine.startTime,
            routine.roomNumber
        )
    }

    private fun clearMessageAfterDelay() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _message.value = ""
        }
    }

    fun clearMessage() {
        _message.value = ""
    }
}