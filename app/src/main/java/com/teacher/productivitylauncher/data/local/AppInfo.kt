package com.teacher.productivitylauncher.data.local

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val activityName: String
)