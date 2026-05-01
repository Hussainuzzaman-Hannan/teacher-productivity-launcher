package com.teacher.productivitylauncher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TeacherLauncherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide components here later
    }
}