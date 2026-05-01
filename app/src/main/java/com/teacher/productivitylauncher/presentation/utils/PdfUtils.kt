package com.teacher.productivitylauncher.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfUtils(private val context: Context) {

    // Convert PDF to Images using Android's built-in PdfRenderer
    suspend fun convertPdfToImages(pdfUri: Uri, onProgress: (Int, Int) -> Unit): List<File> = withContext(Dispatchers.IO) {
        val outputFiles = mutableListOf<File>()

        try {
            val inputStream = context.contentResolver.openInputStream(pdfUri)
            val tempFile = File(context.cacheDir, "temp_pdf.pdf")
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            inputStream?.close()

            val fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val outputFile = File(context.cacheDir, "page_${i + 1}.jpg")
                FileOutputStream(outputFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                outputFiles.add(outputFile)

                page.close()
                onProgress(i + 1, renderer.pageCount)
            }

            renderer.close()
            fileDescriptor.close()
            tempFile.delete()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        outputFiles
    }
}