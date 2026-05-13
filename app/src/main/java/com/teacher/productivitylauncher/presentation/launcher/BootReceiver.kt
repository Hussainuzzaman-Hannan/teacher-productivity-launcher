package com.teacher.productivitylauncher.presentation.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // ফোন restart হলে সব alarm আবার schedule করো
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ClassReminderService.createNotificationChannel(context)
                    val manager = ClassReminderManager(context)
                    manager.scheduleAllReminders()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}