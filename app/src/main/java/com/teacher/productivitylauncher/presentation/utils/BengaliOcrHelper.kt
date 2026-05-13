package com.teacher.productivitylauncher.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BengaliOcrHelper(private val context: Context) {

    private var tessBaseAPI: TessBaseAPI? = null
    private var isInitialized = false

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val dataPath = context.filesDir.absolutePath
            val tessDataDir = File(dataPath, "tessdata")
            if (!tessDataDir.exists()) {
                tessDataDir.mkdirs()
            }

            // assets থেকে tessdata copy করো
            copyTrainedData("ben.traineddata", tessDataDir)
            copyTrainedData("eng.traineddata", tessDataDir)

            tessBaseAPI = TessBaseAPI()
            isInitialized = tessBaseAPI!!.init(dataPath, "ben+eng")
            isInitialized
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyTrainedData(fileName: String, tessDataDir: File) {
        val destFile = File(tessDataDir, fileName)
        if (!destFile.exists()) {
            context.assets.open("tessdata/$fileName").use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    suspend fun extractText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) {
                val success = initialize()
                if (!success) return@withContext "Failed to initialize OCR engine"
            }
            tessBaseAPI?.setImage(bitmap)
            val result = tessBaseAPI?.utF8Text ?: "No text found"
            tessBaseAPI?.clear()
            result.trim().ifBlank { "No text found" }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun release() {
        tessBaseAPI?.recycle()
        tessBaseAPI = null
        isInitialized = false
    }
}