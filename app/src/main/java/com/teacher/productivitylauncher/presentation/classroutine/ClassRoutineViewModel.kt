package com.teacher.productivitylauncher.presentation.classroutine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.productivitylauncher.data.local.SettingsDataStore
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.ClassRoutine
import com.teacher.productivitylauncher.data.local.repository.ClassRoutineRepository
import com.teacher.productivitylauncher.presentation.launcher.ClassReminderManager
import com.teacher.productivitylauncher.presentation.launcher.ClassReminderService
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
    private val reminderManager = ClassReminderManager(application)
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
        // Notification channel তৈরি করো
        ClassReminderService.createNotificationChannel(application)
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

    fun addRoutine(
        subjectName: String,
        className: String,
        teacherName: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        roomNumber: String
    ) {
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
            // insert করে নতুন id পাও
            val insertedId = repository.insertRoutine(routine)
            val routineWithId = routine.copy(id = insertedId.toInt())

            _message.value = "Class added successfully"
            loadAvailableClasses()
            loadTodayRoutines()
            clearMessageAfterDelay()

            // সঠিক id দিয়ে alarm schedule করো
            scheduleAlarm(routineWithId)
        }
    }

    fun updateRoutine(routine: ClassRoutine) {
        viewModelScope.launch {
            repository.updateRoutine(routine)
            _message.value = "Class updated successfully"
            loadTodayRoutines()
            clearMessageAfterDelay()

            // পুরনো alarm cancel করে নতুন করে schedule করো
            if (routine.notificationEnabled && areNotificationsEnabled) {
                scheduleAlarm(routine)
            } else {
                reminderManager.cancelRoutineReminder(routine.id)
            }
        }
    }

    fun deleteRoutine(routine: ClassRoutine) {
        viewModelScope.launch {
            // আগে alarm cancel করো
            reminderManager.cancelRoutineReminder(routine.id)

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

            // Toggle অনুযায়ী alarm schedule বা cancel করো
            if (updated.notificationEnabled && areNotificationsEnabled) {
                scheduleAlarm(updated)
            } else {
                reminderManager.cancelRoutineReminder(updated.id)
            }
        }
    }

    private fun scheduleAlarm(routine: ClassRoutine) {
        if (!areNotificationsEnabled) return
        val context = getApplication<Application>()
        val calendarDay = when (routine.dayOfWeek) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
        ClassReminderService.scheduleClassReminders(
            context = context,
            routineId = routine.id,
            subjectName = routine.subjectName,
            startTime = routine.startTime,
            endTime = routine.endTime,
            dayOfWeek = calendarDay
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