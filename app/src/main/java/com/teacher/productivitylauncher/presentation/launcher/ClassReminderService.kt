package com.teacher.productivitylauncher.presentation.launcher

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.teacher.productivitylauncher.R
import java.util.Calendar

class ClassReminderService {

    companion object {
        const val CHANNEL_ID = "class_reminder_channel"
        private const val CHANNEL_NAME = "Class Reminders"
        internal const val NOTIFICATION_ID_START = 1000
        internal const val NOTIFICATION_ID_END = 2000

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for class start and end times"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                }
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }

        /**
         * "9:00 AM", "10:30 PM" ইত্যাদি format parse করে
         * 24-hour format এ hour ও minute return করে
         */
        fun parseTime(time: String): Pair<Int, Int>? {
            return try {
                val clean = time.trim().uppercase()
                val isPm = clean.endsWith("PM")
                val isAm = clean.endsWith("AM")

                val timePart = clean
                    .replace("AM", "")
                    .replace("PM", "")
                    .trim()

                val parts = timePart.split(":")
                if (parts.size < 2) return null

                var hour = parts[0].trim().toInt()
                val minute = parts[1].trim().toInt()

                // 12-hour → 24-hour convert
                if (isPm && hour != 12) hour += 12
                if (isAm && hour == 12) hour = 0

                Pair(hour, minute)
            } catch (e: Exception) {
                android.util.Log.e("ClassReminderService", "Time parse error: $time", e)
                null
            }
        }

        fun canScheduleExactAlarms(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        }

        fun scheduleClassReminders(
            context: Context,
            routineId: Int,
            subjectName: String,
            startTime: String,
            endTime: String,
            dayOfWeek: Int
        ) {
            if (!canScheduleExactAlarms(context)) {
                android.util.Log.w("ClassReminderService", "Cannot schedule exact alarms — permission not granted")
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // ক্লাস শুরুর ৫ মিনিট আগে reminder
            scheduleReminder(
                context = context,
                alarmManager = alarmManager,
                routineId = routineId,
                subjectName = subjectName,
                time = startTime,
                dayOfWeek = dayOfWeek,
                isStartReminder = true,
                minutesBefore = 5
            )

            // ক্লাস শেষ হওয়ার সময় reminder
            scheduleReminder(
                context = context,
                alarmManager = alarmManager,
                routineId = routineId,
                subjectName = subjectName,
                time = endTime,
                dayOfWeek = dayOfWeek,
                isStartReminder = false,
                minutesBefore = 0
            )
        }

        private fun scheduleReminder(
            context: Context,
            alarmManager: AlarmManager,
            routineId: Int,
            subjectName: String,
            time: String,
            dayOfWeek: Int,
            isStartReminder: Boolean,
            minutesBefore: Int
        ) {
            val parsed = parseTime(time) ?: return
            var (hour, minute) = parsed

            // minutesBefore বাদ দাও
            minute -= minutesBefore
            if (minute < 0) {
                minute += 60
                hour -= 1
                if (hour < 0) hour += 24
            }

            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // সময় পেরিয়ে গেলে পরের সপ্তাহে schedule করো
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }

            // unique request code — routineId + type
            val requestCode = if (isStartReminder) {
                routineId * 10 + 1
            } else {
                routineId * 10 + 2
            }

            val intent = Intent(context, ClassReminderReceiver::class.java).apply {
                putExtra("subject_name", subjectName)
                putExtra("is_start", isStartReminder)
                putExtra("time", time)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                android.util.Log.d(
                    "ClassReminderService",
                    "Alarm scheduled: $subjectName ${if (isStartReminder) "START" else "END"} at ${calendar.time}"
                )
            } catch (e: SecurityException) {
                android.util.Log.e("ClassReminderService", "SecurityException scheduling alarm", e)
            }
        }

        fun cancelClassReminders(context: Context, routineId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            listOf(routineId * 10 + 1, routineId * 10 + 2).forEach { requestCode ->
                val intent = Intent(context, ClassReminderReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                pendingIntent?.let { alarmManager.cancel(it) }
            }
        }
    }
}

class ClassReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subjectName = intent.getStringExtra("subject_name") ?: return
        val isStart = intent.getBooleanExtra("is_start", true)
        val time = intent.getStringExtra("time") ?: ""

        // Notification channel নিশ্চিত করো
        ClassReminderService.createNotificationChannel(context)
        showNotification(context, subjectName, isStart, time)
    }

    private fun showNotification(
        context: Context,
        subjectName: String,
        isStart: Boolean,
        time: String
    ) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val title = if (isStart) "📚 ক্লাস শুরু হতে ৫ মিনিট!" else "⏰ ক্লাস শেষ"
        val content = if (isStart) {
            "$subjectName ক্লাস $time এ শুরু হবে"
        } else {
            "$subjectName ক্লাস $time এ শেষ হচ্ছে"
        }

        val notification = NotificationCompat.Builder(context, ClassReminderService.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        val notificationId = if (isStart) {
            ClassReminderService.NOTIFICATION_ID_START + subjectName.hashCode()
        } else {
            ClassReminderService.NOTIFICATION_ID_END + subjectName.hashCode()
        }

        notificationManager.notify(notificationId, notification)
    }
}