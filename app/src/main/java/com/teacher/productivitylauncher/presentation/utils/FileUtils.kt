package com.teacher.productivitylauncher.presentation.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class FileUtils(private val context: Context) {

    fun saveBitmapToFile(bitmap: Bitmap, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file
    }

    fun bitmapToFile(bitmap: Bitmap, fileName: String): File {
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val contentResolver: ContentResolver = context.contentResolver
            BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}