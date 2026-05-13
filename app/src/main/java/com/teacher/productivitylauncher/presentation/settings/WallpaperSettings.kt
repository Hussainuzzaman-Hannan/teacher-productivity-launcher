package com.teacher.productivitylauncher.presentation.settings

import android.graphics.Bitmap
import android.net.Uri

data class WallpaperSettings(
    val wallpaperUri: String? = null,
    val useSameForLockScreen: Boolean = true,  // হোম ও লকস্ক্রিন একই রাখবে
    val blurAmount: Float = 0f,  // ব্লার ইফেক্ট (0-25)
    val darkOverlay: Float = 0.3f  // ডার্ক ওভারলে (0-1)
) {
    companion object {
        fun default() = WallpaperSettings()
    }
}