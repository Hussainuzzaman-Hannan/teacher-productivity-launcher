package com.teacher.productivitylauncher.presentation.launcher

import android.content.Context
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class ClassReminderManager(private val context: Context) {

    private val database = TeacherDatabase.getDatabase(context)

    suspend fun scheduleAllReminders() {
        withContext(Dispatchers.IO) {
            try {
                val routines = database.classRoutineDao().getAllRoutines()

                routines.forEach { routine ->
                    if (routine.notificationEnabled) {
                        val dayOfWeek = getDayOfWeekCalendarValue(routine.dayOfWeek)
                        ClassReminderService.scheduleClassReminders(
                            context = context,
                            routineId = routine.id,
                            subjectName = routine.subjectName,
                            startTime = routine.startTime,
                            endTime = routine.endTime,
                            dayOfWeek = dayOfWeek
                        )
                    }
                }

                android.util.Log.d("ClassReminderManager", "Scheduled ${routines.size} class reminders")
            } catch (e: Exception) {
                android.util.Log.e("ClassReminderManager", "Error scheduling reminders", e)
            }
        }
    }

    suspend fun cancelRoutineReminder(routineId: Int) {
        withContext(Dispatchers.IO) {
            ClassReminderService.cancelClassReminders(context, routineId)
        }
    }

    private fun getDayOfWeekCalendarValue(day: Int): Int {
        return when (day) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
    }
}