package com.teacher.productivitylauncher.presentation.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import com.teacher.productivitylauncher.presentation.receiver.AdminReceiver
import kotlinx.coroutines.*

class LockScreenUtils(private val context: Context) {

    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent: ComponentName = ComponentName(context, AdminReceiver::class.java)

    fun isDeviceAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    fun requestDeviceAdmin(): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for screen lock functionality")
        }
    }

    fun lockDevice() {
        if (isDeviceAdminActive()) {
            devicePolicyManager.lockNow()
        }
    }

    /**
     * Screen Dim + System Lock Method (Fingerprint Friendly)
     * This method dims the screen and lets the system lock naturally
     */
    fun lockWithScreenDim() {
        try {
            // Reduce screen timeout to minimum (5 seconds)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    if (Settings.System.canWrite(context)) {
                        Settings.System.putInt(
                            context.contentResolver,
                            Settings.System.SCREEN_OFF_TIMEOUT,
                            5000 // 5 seconds
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Dim the screen brightness to minimum
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val layoutParams = windowManager.defaultDisplay?.let {
                WindowManager.LayoutParams().apply {
                    screenBrightness = 0.01f // Minimum brightness
                }
            }

            // Use admin lock as fallback after a delay
            // This ensures screen locks even if system doesn't
            GlobalScope.launch {
                delay(5500) // Just after screen timeout
                if (isDeviceAdminActive()) {
                    devicePolicyManager.lockNow()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            lockDevice()
        }
    }

    /**
     * Smart Lock - Tries multiple methods
     */
    fun smartLock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Use screen dim method
            lockWithScreenDim()
        } else {
            // Older Android - Use admin lock
            lockDevice()
        }
    }

    fun wakeUpDevice() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "TeacherLauncher:WakeLockTag"
        )
        wakeLock.acquire(1000)
        wakeLock.release()
    }
}