package com.teacher.productivitylauncher.presentation.tools

import androidx.compose.foundation.background
import android.Manifest
import android.content.ContentValues
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.teacher.productivitylauncher.presentation.utils.BengaliOcrHelper
import com.teacher.productivitylauncher.presentation.utils.PdfUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

fun shareFile(context: android.content.Context, file: java.io.File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareMultipleFiles(context: android.content.Context, files: List<java.io.File>) {
    try {
        val uris = ArrayList<Uri>()
        files.forEach { file ->
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            uris.add(uri)
        }
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share ${files.size} Images"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing files: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun sharePdfFile(context: android.content.Context, file: java.io.File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun saveImagesToGallery(context: android.content.Context, files: List<java.io.File>) {
    try {
        var savedCount = 0
        files.forEach { file ->
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TeacherLauncher")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
            )
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    file.inputStream().copyTo(outputStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(it, contentValues, null, null)
                }
                savedCount++
            }
        }
        Toast.makeText(context, "✅ $savedCount images saved to Gallery/TeacherLauncher", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving to gallery: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("OCR Text", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "✅ Text copied to clipboard", Toast.LENGTH_SHORT).show()
}

fun saveTextToFile(context: Context, text: String) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "OCR_$timeStamp.txt"
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/TeacherLauncher")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
            )
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(text.toByteArray())
                }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(it, contentValues, null, null)
                Toast.makeText(context, "✅ Saved to Download/TeacherLauncher/$fileName", Toast.LENGTH_LONG).show()
            }
        } else {
            val file = File(context.getExternalFilesDir(null), fileName)
            file.writeText(text)
            Toast.makeText(context, "✅ Saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving text: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun TeachingToolsScreen(
    onClose: () -> Unit,
    onClassRoutineClick: () -> Unit = {},
    onNotesClick: () -> Unit = {},
    onExamsClick: () -> Unit = {},
    onQuestionMakerClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pdfUtils = remember { PdfUtils(context) }
    val bengaliOcrHelper = remember { BengaliOcrHelper(context) }

    var showCalculator by remember { mutableStateOf(false) }
    var extractedText by remember { mutableStateOf<String?>(null) }
    var showProgress by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    var progressCurrent by remember { mutableStateOf(0) }
    var progressTotal by remember { mutableStateOf(0) }
    var isBengaliInitializing by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    val photoFile = remember { File(context.cacheDir, "ocr_capture.jpg") }
    val photoUri = remember {
        androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", photoFile
        )
    }

    val bengaliPhotoFile = remember { File(context.cacheDir, "ocr_bengali_capture.jpg") }
    val bengaliPhotoUri = remember {
        androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", bengaliPhotoFile
        )
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    showProgress = true
                    progressMessage = "Converting PDF to images..."
                    val images = pdfUtils.convertPdfToImages(it) { current, total ->
                        progressCurrent = current
                        progressTotal = total
                        progressMessage = "Converting page $current of $total..."
                    }
                    showProgress = false
                    if (images.isNotEmpty()) {
                        saveImagesToGallery(context, images)
                        if (images.size == 1) shareFile(context, images.first())
                        else shareMultipleFiles(context, images)
                    } else {
                        Toast.makeText(context, "Failed to convert PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    val jpgToPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                scope.launch {
                    showProgress = true
                    progressMessage = "Converting images to PDF..."
                    progressCurrent = 0
                    progressTotal = uris.size
                    try {
                        val pdfFile = pdfUtils.convertImagesToPdf(uris) { current, total ->
                            progressCurrent = current
                            progressTotal = total
                            progressMessage = "Processing image $current of $total..."
                        }
                        showProgress = false
                        if (pdfFile != null) {
                            Toast.makeText(context, "✅ PDF created successfully!", Toast.LENGTH_LONG).show()
                            sharePdfFile(context, pdfFile)
                        } else {
                            Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        showProgress = false
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    val highResCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                scope.launch {
                    showProgress = true
                    progressMessage = "Extracting text from image..."
                    try {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                        bitmap?.let { bmp ->
                            val image = InputImage.fromBitmap(bmp, 0)
                            val deferred = CompletableDeferred<String>()
                            recognizer.process(image)
                                .addOnSuccessListener { visionText -> deferred.complete(visionText.text) }
                                .addOnFailureListener { e -> deferred.complete("Error: ${e.message}") }
                            val resultText = deferred.await()
                            extractedText = if (resultText.isBlank()) "No text found in image" else resultText
                            bmp.recycle()
                        } ?: run { extractedText = "Failed to load image" }
                    } catch (e: Exception) {
                        extractedText = "Error: ${e.message}"
                    }
                    showProgress = false
                }
            }
        }
    )

    val multipleGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                scope.launch {
                    showProgress = true
                    progressTotal = uris.size
                    val allTexts = mutableListOf<String>()
                    uris.forEachIndexed { index, uri ->
                        progressCurrent = index + 1
                        progressMessage = "Processing image ${index + 1} of ${uris.size}..."
                        try {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()
                            bitmap?.let { bmp ->
                                val image = InputImage.fromBitmap(bmp, 0)
                                val deferred = CompletableDeferred<String>()
                                recognizer.process(image)
                                    .addOnSuccessListener { visionText -> deferred.complete(visionText.text) }
                                    .addOnFailureListener { e -> deferred.complete("Error: ${e.message}") }
                                val resultText = deferred.await()
                                if (resultText.isNotBlank() && !resultText.startsWith("Error")) {
                                    allTexts.add("--- Image ${index + 1} ---\n$resultText")
                                }
                                bmp.recycle()
                            }
                        } catch (e: Exception) {
                            allTexts.add("--- Image ${index + 1} --- Error: ${e.message}")
                        }
                    }
                    showProgress = false
                    extractedText = if (allTexts.isEmpty()) "No text found in any image"
                    else allTexts.joinToString("\n\n")
                }
            }
        }
    )

    val bengaliCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                scope.launch {
                    showProgress = true
                    progressMessage = "Initializing Bengali OCR..."
                    val initialized = bengaliOcrHelper.initialize()
                    if (!initialized) {
                        showProgress = false
                        Toast.makeText(context, "Failed to initialize Bengali OCR", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    progressMessage = "Extracting Bengali text..."
                    try {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(bengaliPhotoFile.absolutePath)
                        bitmap?.let { bmp ->
                            extractedText = bengaliOcrHelper.extractText(bmp)
                            bmp.recycle()
                        } ?: run { extractedText = "Failed to load image" }
                    } catch (e: Exception) {
                        extractedText = "Error: ${e.message}"
                    }
                    showProgress = false
                }
            }
        }
    )

    val bengaliGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                scope.launch {
                    if (isBengaliInitializing) return@launch
                    isBengaliInitializing = true
                    showProgress = true
                    progressTotal = uris.size
                    progressMessage = "Initializing Bengali OCR..."
                    val initialized = bengaliOcrHelper.initialize()
                    if (!initialized) {
                        showProgress = false
                        isBengaliInitializing = false
                        Toast.makeText(context, "Failed to initialize Bengali OCR", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    isBengaliInitializing = false
                    val allTexts = mutableListOf<String>()
                    uris.forEachIndexed { index, uri ->
                        progressCurrent = index + 1
                        progressMessage = "Processing image ${index + 1} of ${uris.size}..."
                        try {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()
                            bitmap?.let { bmp ->
                                val resultText = bengaliOcrHelper.extractText(bmp)
                                if (resultText.isNotBlank() && resultText != "No text found") {
                                    allTexts.add("--- Image ${index + 1} ---\n$resultText")
                                }
                                bmp.recycle()
                            }
                        } catch (e: Exception) {
                            allTexts.add("--- Image ${index + 1} --- Error: ${e.message}")
                        }
                    }
                    showProgress = false
                    extractedText = if (allTexts.isEmpty()) "No Bengali text found"
                    else allTexts.joinToString("\n\n")
                }
            }
        }
    )

    // Full Screen Dialog
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("🛠️ Teaching Tools", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 📚 Classroom Tools Section
                    item {
                        Text(
                            text = "📚 Classroom Tools",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                        )
                    }

                    item {
                        ToolCard(
                            title = "Class Routine",
                            description = "Manage daily class schedule",
                            icon = Icons.Default.Schedule,
                            onClick = onClassRoutineClick
                        )
                    }

                    item {
                        ToolCard(
                            title = "Quick Notes",
                            description = "Create and manage notes",
                            icon = Icons.Default.Note,
                            onClick = onNotesClick
                        )
                    }

                    // 📝 Exam Tools Section
                    item {
                        Text(
                            text = "📝 Exam Tools",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                        )
                    }

                    item {
                        ToolCard(
                            title = "Exams",
                            description = "Manage and schedule exams",
                            icon = Icons.Default.DateRange,
                            onClick = onExamsClick
                        )
                    }

                    item {
                        ToolCard(
                            title = "Question Maker",
                            description = "Create exam questions easily",
                            icon = Icons.Default.Edit,
                            onClick = onQuestionMakerClick
                        )
                    }

                    // 🔧 Utility Tools Section
                    item {
                        Text(
                            text = "🔧 Utility Tools",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                        )
                    }

                    item {
                        ToolCard(
                            title = "PDF to JPG",
                            description = "Convert PDF pages to images",
                            icon = Icons.Default.PictureAsPdf,
                            onClick = { pdfPickerLauncher.launch("application/pdf") }
                        )
                    }

                    item {
                        ToolCard(
                            title = "JPG to PDF",
                            description = "Convert images to PDF",
                            icon = Icons.Default.Image,
                            onClick = { jpgToPdfLauncher.launch("image/*") }
                        )
                    }

                    item {
                        ToolCard(
                            title = "OCR Camera",
                            description = "Extract text from camera",
                            icon = Icons.Default.CameraAlt,
                            onClick = {
                                if (cameraPermissionState.status.isGranted) highResCameraLauncher.launch(photoUri)
                                else cameraPermissionState.launchPermissionRequest()
                            }
                        )
                    }

                    item {
                        ToolCard(
                            title = "OCR Gallery",
                            description = "Extract text from images",
                            icon = Icons.Default.Image,
                            onClick = { multipleGalleryLauncher.launch("image/*") }
                        )
                    }

                    item {
                        ToolCard(
                            title = "বাংলা OCR Camera",
                            description = "ক্যামেরা থেকে বাংলা টেক্সট",
                            icon = Icons.Default.Translate,
                            onClick = {
                                if (cameraPermissionState.status.isGranted) bengaliCameraLauncher.launch(bengaliPhotoUri)
                                else cameraPermissionState.launchPermissionRequest()
                            }
                        )
                    }

                    item {
                        ToolCard(
                            title = "বাংলা OCR Gallery",
                            description = "ছবি থেকে বাংলা টেক্সট",
                            icon = Icons.Default.Translate,
                            onClick = { bengaliGalleryLauncher.launch("image/*") }
                        )
                    }

                    item {
                        ToolCard(
                            title = "Calculator",
                            description = "Basic, Scientific & Unit Converter",
                            icon = Icons.Default.Calculate,
                            onClick = { showCalculator = true }
                        )
                    }
                }
            }
        }
    }

    // Progress Dialog
    if (showProgress) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Processing...") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = if (progressTotal > 0) progressCurrent.toFloat() / progressTotal.toFloat() else 0f
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(progressMessage, fontSize = 12.sp)
                }
            },
            confirmButton = {}
        )
    }

    // Extracted Text Dialog
    if (extractedText != null) {
        AlertDialog(
            onDismissRequest = { extractedText = null },
            title = { Text("📄 Extracted Text") },
            text = {
                Column {
                    Box(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(extractedText!!, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { copyToClipboard(context, extractedText!!) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy")
                        }
                        Button(
                            onClick = { saveTextToFile(context, extractedText!!) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, extractedText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Text"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { extractedText = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Calculator Screen
    if (showCalculator) {
        CalculatorScreen(onBack = { showCalculator = false })
    }
}

@Composable
fun ToolCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ============================================================
// CALCULATOR SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("সাধারণ", "Scientific", "Unit Converter")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🧮 Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp) }
                    )
                }
            }

            when (selectedTab) {
                0 -> BasicCalculator()
                1 -> ScientificCalculator()
                2 -> UnitConverter()
            }
        }
    }
}

// ============================================================
// Basic Calculator
// ============================================================

@Composable
fun BasicCalculator() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var justEvaluated by remember { mutableStateOf(false) }

    fun evaluate(expr: String): String {
        return try {
            val tokens = expr.split(Regex("(?<=[+\\-×÷])|(?=[+\\-×÷])"))
            var current = tokens[0].replace(",", ".").toDouble()
            var i = 1
            while (i < tokens.size) {
                val op = tokens[i]
                val next = tokens[i + 1].replace(",", ".").toDouble()
                current = when (op) {
                    "+" -> current + next
                    "-" -> current - next
                    "×" -> current * next
                    "÷" -> current / next
                    else -> current
                }
                i += 2
            }
            if (current == current.toLong().toDouble()) current.toLong().toString()
            else "%.10f".format(current).trimEnd('0').trimEnd('.')
        } catch (e: Exception) { "Error" }
    }

    fun onButton(btn: String) {
        when (btn) {
            "AC" -> { expression = ""; result = ""; justEvaluated = false }
            "⌫" -> {
                if (justEvaluated) { expression = ""; result = ""; justEvaluated = false }
                else if (expression.isNotEmpty()) expression = expression.dropLast(1)
            }
            "=" -> {
                if (expression.isNotEmpty()) {
                    result = evaluate(expression)
                    justEvaluated = true
                }
            }
            "%" -> {
                val num = expression.toDoubleOrNull()
                if (num != null) {
                    expression = "%.10f".format(num / 100).trimEnd('0').trimEnd('.')
                    result = ""
                    justEvaluated = false
                }
            }
            "()" -> {
                val openCount = expression.count { it == '(' }
                val closeCount = expression.count { it == ')' }
                expression += if (openCount == closeCount || expression.isEmpty() ||
                    expression.last() in listOf('+', '-', '×', '÷', '(')) "(" else ")"
                justEvaluated = false
            }
            "+", "-", "×", "÷" -> {
                if (justEvaluated) { expression = result + btn; result = ""; justEvaluated = false }
                else if (expression.isNotEmpty() && expression.last() !in listOf('+', '-', '×', '÷')) expression += btn
            }
            "." -> {
                if (justEvaluated) { expression = "0."; result = ""; justEvaluated = false }
                else {
                    val lastNum = expression.split(Regex("[+\\-×÷]")).lastOrNull() ?: ""
                    if (!lastNum.contains('.'))
                        expression += if (expression.isEmpty() || expression.last() in listOf('+', '-', '×', '÷')) "0." else "."
                }
            }
            else -> {
                if (justEvaluated) { expression = btn; result = ""; justEvaluated = false }
                else expression += btn
                val liveResult = evaluate(expression)
                if (liveResult != "Error" && liveResult != expression) result = liveResult
            }
        }
    }

    val rows = listOf(
        listOf("AC", "()", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "⌫", "=")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (result.isNotEmpty()) {
                    AutoSizeText(
                        text = expression,
                        maxFontSize = 28.sp,
                        minFontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    AutoSizeText(
                        text = result,
                        maxFontSize = 56.sp,
                        minFontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    AutoSizeText(
                        text = expression.ifEmpty { "0" },
                        maxFontSize = 56.sp,
                        minFontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { btn ->
                        val isOperator = btn in listOf("+", "-", "×", "÷")
                        val isEquals = btn == "="
                        val isSpecial = btn in listOf("AC", "()", "%")
                        val isClear = btn == "⌫"

                        val bgColor = when {
                            isEquals -> MaterialTheme.colorScheme.primary
                            isOperator -> MaterialTheme.colorScheme.primaryContainer
                            isSpecial || isClear -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val contentColor = when {
                            isEquals -> MaterialTheme.colorScheme.onPrimary
                            isOperator -> MaterialTheme.colorScheme.primary
                            isSpecial || isClear -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Button(
                            onClick = { onButton(btn) },
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = bgColor,
                                contentColor = contentColor
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = btn,
                                fontSize = 22.sp,
                                fontWeight = if (isOperator || isEquals) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ============================================================
// Scientific Calculator
// ============================================================

@Composable
fun ScientificCalculator() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var isDegree by remember { mutableStateOf(true) }
    var justEvaluated by remember { mutableStateOf(false) }

    fun toRad(v: Double) = if (isDegree) Math.toRadians(v) else v

    fun evaluate(expr: String): String {
        return try {
            val e = expr.replace("×", "*").replace("÷", "/")
                .replace("π", Math.PI.toString()).replace("e", Math.E.toString())
            val tokens = e.split(Regex("(?<=[+\\-*/])|(?=[+\\-*/])"))
            var current = tokens[0].toDouble()
            var i = 1
            while (i < tokens.size) {
                val op = tokens[i]
                val next = tokens[i + 1].toDouble()
                current = when (op) {
                    "+" -> current + next
                    "-" -> current - next
                    "*" -> current * next
                    "/" -> current / next
                    else -> current
                }
                i += 2
            }
            if (current == current.toLong().toDouble()) current.toLong().toString()
            else "%.10f".format(current).trimEnd('0').trimEnd('.')
        } catch (e: Exception) { "" }
    }

    fun applyFunc(func: String) {
        val value = expression.toDoubleOrNull() ?: result.toDoubleOrNull() ?: return
        val res = try {
            when (func) {
                "sin" -> sin(toRad(value))
                "cos" -> cos(toRad(value))
                "tan" -> tan(toRad(value))
                "sin⁻¹" -> if (isDegree) Math.toDegrees(asin(value)) else asin(value)
                "cos⁻¹" -> if (isDegree) Math.toDegrees(acos(value)) else acos(value)
                "tan⁻¹" -> if (isDegree) Math.toDegrees(atan(value)) else atan(value)
                "log" -> log10(value)
                "ln" -> ln(value)
                "√" -> sqrt(value)
                "x²" -> value.pow(2)
                "x³" -> value.pow(3)
                "1/x" -> 1.0 / value
                "n!" -> { var f = 1.0; for (i in 1..value.toInt()) f *= i; f }
                else -> value
            }
        } catch (e: Exception) { return }
        val resStr = if (res == res.toLong().toDouble()) res.toLong().toString()
        else "%.10f".format(res).trimEnd('0').trimEnd('.')
        expression = resStr
        result = ""
        justEvaluated = true
    }

    fun onButton(btn: String) {
        when (btn) {
            "AC" -> { expression = ""; result = ""; justEvaluated = false }
            "⌫" -> {
                if (justEvaluated) { expression = ""; result = ""; justEvaluated = false }
                else if (expression.isNotEmpty()) expression = expression.dropLast(1)
            }
            "=" -> {
                if (expression.isNotEmpty()) {
                    val r = evaluate(expression)
                    if (r.isNotEmpty()) { result = r; justEvaluated = true }
                }
            }
            "%" -> {
                val num = expression.toDoubleOrNull()
                if (num != null) expression = "%.10f".format(num / 100).trimEnd('0').trimEnd('.')
            }
            "()" -> {
                val open = expression.count { it == '(' }
                val close = expression.count { it == ')' }
                expression += if (open == close || expression.isEmpty() ||
                    expression.last() in listOf('+', '-', '×', '÷', '(')) "(" else ")"
                justEvaluated = false
            }
            "π" -> { expression += "π"; justEvaluated = false }
            "e" -> { expression += "e"; justEvaluated = false }
            "+", "-", "×", "÷" -> {
                if (justEvaluated) { expression = result + btn; result = ""; justEvaluated = false }
                else if (expression.isNotEmpty() && expression.last() !in listOf('+', '-', '×', '÷')) expression += btn
            }
            "." -> {
                val lastNum = expression.split(Regex("[+\\-×÷]")).lastOrNull() ?: ""
                if (!lastNum.contains('.'))
                    expression += if (expression.isEmpty() || expression.last() in listOf('+', '-', '×', '÷')) "0." else "."
                justEvaluated = false
            }
            "sin", "cos", "tan", "sin⁻¹", "cos⁻¹", "tan⁻¹",
            "log", "ln", "√", "x²", "x³", "1/x", "n!" -> applyFunc(btn)
            else -> {
                if (justEvaluated) { expression = btn; result = ""; justEvaluated = false }
                else expression += btn
                val liveResult = evaluate(expression)
                if (liveResult.isNotEmpty()) result = liveResult
            }
        }
    }

    val funcRows = listOf(
        listOf("sin", "cos", "tan", "log", "ln"),
        listOf("sin⁻¹", "cos⁻¹", "tan⁻¹", "√", "n!"),
        listOf("x²", "x³", "1/x", "π", "e")
    )

    val numRows = listOf(
        listOf("AC", "()", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "⌫", "=")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.85f)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isDegree,
                        onClick = { isDegree = true },
                        label = { Text("DEG", fontSize = 11.sp) },
                        modifier = Modifier.height(32.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = !isDegree,
                        onClick = { isDegree = false },
                        label = { Text("RAD", fontSize = 11.sp) },
                        modifier = Modifier.height(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                AutoSizeText(
                    text = expression.ifEmpty { "0" },
                    maxFontSize = 36.sp,
                    minFontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (result.isNotEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                if (result.isNotEmpty()) {
                    AutoSizeText(
                        text = result,
                        maxFontSize = 48.sp,
                        minFontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            funcRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { btn ->
                        OutlinedButton(
                            onClick = { onButton(btn) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text(btn, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            numRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { btn ->
                        val isOperator = btn in listOf("+", "-", "×", "÷")
                        val isEquals = btn == "="
                        val isSpecial = btn in listOf("AC", "()", "%")
                        val isClear = btn == "⌫"

                        val bgColor = when {
                            isEquals -> MaterialTheme.colorScheme.primary
                            isOperator -> MaterialTheme.colorScheme.primaryContainer
                            isSpecial || isClear -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val contentColor = when {
                            isEquals -> MaterialTheme.colorScheme.onPrimary
                            isOperator -> MaterialTheme.colorScheme.primary
                            isSpecial || isClear -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Button(
                            onClick = { onButton(btn) },
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = bgColor,
                                contentColor = contentColor
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(btn, fontSize = 20.sp, fontWeight = if (isOperator || isEquals) FontWeight.Medium else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// ============================================================
// Unit Converter
// ============================================================

@Composable
fun UnitConverter() {
    val categories = listOf("Length", "Weight", "Temperature", "Area", "Speed")
    var selectedCategory by remember { mutableStateOf("Length") }
    var inputValue by remember { mutableStateOf("") }
    var fromUnit by remember { mutableStateOf("") }
    var toUnit by remember { mutableStateOf("") }
    var convertedResult by remember { mutableStateOf("") }

    val unitMap = mapOf(
        "Length" to listOf("meter", "km", "cm", "mm", "mile", "yard", "foot", "inch"),
        "Weight" to listOf("kg", "gram", "mg", "pound", "ounce", "ton"),
        "Temperature" to listOf("Celsius", "Fahrenheit", "Kelvin"),
        "Area" to listOf("m²", "km²", "cm²", "hectare", "acre", "ft²"),
        "Speed" to listOf("m/s", "km/h", "mph", "knot")
    )

    fun convert(value: Double, from: String, to: String, category: String): Double {
        if (from == to) return value
        return when (category) {
            "Temperature" -> when {
                from == "Celsius" && to == "Fahrenheit" -> value * 9 / 5 + 32
                from == "Celsius" && to == "Kelvin" -> value + 273.15
                from == "Fahrenheit" && to == "Celsius" -> (value - 32) * 5 / 9
                from == "Fahrenheit" && to == "Kelvin" -> (value - 32) * 5 / 9 + 273.15
                from == "Kelvin" && to == "Celsius" -> value - 273.15
                from == "Kelvin" && to == "Fahrenheit" -> (value - 273.15) * 9 / 5 + 32
                else -> value
            }
            else -> {
                val toBase = mapOf(
                    "meter" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001,
                    "mile" to 1609.344, "yard" to 0.9144, "foot" to 0.3048, "inch" to 0.0254,
                    "kg" to 1.0, "gram" to 0.001, "mg" to 0.000001,
                    "pound" to 0.453592, "ounce" to 0.0283495, "ton" to 1000.0,
                    "m²" to 1.0, "km²" to 1_000_000.0, "cm²" to 0.0001,
                    "hectare" to 10_000.0, "acre" to 4046.856, "ft²" to 0.092903,
                    "m/s" to 1.0, "km/h" to 0.277778, "mph" to 0.44704, "knot" to 0.514444
                )
                val fromFactor = toBase[from] ?: 1.0
                val toFactor = toBase[to] ?: 1.0
                value * fromFactor / toFactor
            }
        }
    }

    val currentUnits = unitMap[selectedCategory] ?: emptyList()

    LaunchedEffect(selectedCategory) {
        fromUnit = currentUnits.firstOrNull() ?: ""
        toUnit = currentUnits.getOrNull(1) ?: ""
        convertedResult = ""
        inputValue = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.height(80.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = inputValue,
            onValueChange = {
                inputValue = it
                val v = it.toDoubleOrNull()
                if (v != null && fromUnit.isNotEmpty() && toUnit.isNotEmpty()) {
                    val res = convert(v, fromUnit, toUnit, selectedCategory)
                    convertedResult = if (res == res.toLong().toDouble()) res.toLong().toString()
                    else "%.4f".format(res).trimEnd('0').trimEnd('.')
                } else convertedResult = ""
            },
            label = { Text("Enter value") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("From", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                currentUnits.forEach { unit ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable {
                                fromUnit = unit
                                val v = inputValue.toDoubleOrNull()
                                if (v != null && toUnit.isNotEmpty()) {
                                    val res = convert(v, fromUnit, toUnit, selectedCategory)
                                    convertedResult = if (res == res.toLong().toDouble()) res.toLong().toString()
                                    else "%.4f".format(res).trimEnd('0').trimEnd('.')
                                }
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = fromUnit == unit, onClick = {
                            fromUnit = unit
                            val v = inputValue.toDoubleOrNull()
                            if (v != null && toUnit.isNotEmpty()) {
                                val res = convert(v, fromUnit, toUnit, selectedCategory)
                                convertedResult = if (res == res.toLong().toDouble()) res.toLong().toString()
                                else "%.4f".format(res).trimEnd('0').trimEnd('.')
                            }
                        })
                        Text(unit, fontSize = 13.sp)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("To", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                currentUnits.forEach { unit ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable {
                                toUnit = unit
                                val v = inputValue.toDoubleOrNull()
                                if (v != null && fromUnit.isNotEmpty()) {
                                    val res = convert(v, fromUnit, toUnit, selectedCategory)
                                    convertedResult = if (res == res.toLong().toDouble()) res.toLong().toString()
                                    else "%.4f".format(res).trimEnd('0').trimEnd('.')
                                }
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = toUnit == unit, onClick = {
                            toUnit = unit
                            val v = inputValue.toDoubleOrNull()
                            if (v != null && fromUnit.isNotEmpty()) {
                                val res = convert(v, fromUnit, toUnit, selectedCategory)
                                convertedResult = if (res == res.toLong().toDouble()) res.toLong().toString()
                                else "%.4f".format(res).trimEnd('0').trimEnd('.')
                            }
                        })
                        Text(unit, fontSize = 13.sp)
                    }
                }
            }
        }

        if (convertedResult.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Result", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(
                        "$inputValue $fromUnit = $convertedResult $toUnit",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ============================================================
// AutoSizeText
// ============================================================

@Composable
fun AutoSizeText(
    text: String,
    maxFontSize: androidx.compose.ui.unit.TextUnit,
    minFontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    modifier: Modifier = Modifier
) {
    var fontSizeValue by remember(text) { mutableStateOf(maxFontSize.value) }
    val minValue = minFontSize.value

    Text(
        text = text,
        fontSize = fontSizeValue.sp,
        fontWeight = fontWeight,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = modifier,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSizeValue > minValue) {
                fontSizeValue = (fontSizeValue * 0.9f).coerceAtLeast(minValue)
            }
        }
    )
}