package com.teacher.productivitylauncher.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfUtils(private val context: Context) {

    // PDF to JPG
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

                // Resolution বাড়াও এবং সাদা background দাও
                val scale = 2
                val bitmap = Bitmap.createBitmap(
                    page.width * scale,
                    page.height * scale,
                    Bitmap.Config.ARGB_8888
                )

                // সাদা background fill করো
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val outputFile = File(context.cacheDir, "page_${i + 1}.jpg")
                FileOutputStream(outputFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                bitmap.recycle()
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

    // JPG to PDF
    suspend fun convertImagesToPdf(
        imageUris: List<Uri>,
        onProgress: (current: Int, total: Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val outputDir = File(context.cacheDir, "pdf_output").apply { mkdirs() }
            val outputFile = File(outputDir, "converted_${System.currentTimeMillis()}.pdf")

            imageUris.forEachIndexed { index, uri ->
                onProgress(index + 1, imageUris.size)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                bitmap?.let { bmp ->
                    val pageWidth = 595
                    val pageHeight = (bmp.height.toFloat() / bmp.width.toFloat() * pageWidth).toInt()
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val scaledBitmap = Bitmap.createScaledBitmap(bmp, pageWidth, pageHeight, true)
                    page.canvas.drawBitmap(scaledBitmap, 0f, 0f, Paint())
                    scaledBitmap.recycle()
                    bmp.recycle()
                    pdfDocument.finishPage(page)
                }
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}