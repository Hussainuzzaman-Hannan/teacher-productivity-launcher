package com.teacher.productivitylauncher.presentation.tools

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.teacher.productivitylauncher.presentation.utils.PdfUtils
import kotlinx.coroutines.launch

data class ToolItem(
    val id: String,
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
)

// Share file function - defined at top level
fun shareFile(context: android.content.Context, file: java.io.File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun TeachingToolsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pdfUtils = remember { PdfUtils(context) }

    var showCalculator by remember { mutableStateOf(false) }
    var extractedText by remember { mutableStateOf<String?>(null) }
    var showProgress by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    var progressCurrent by remember { mutableStateOf(0) }
    var progressTotal by remember { mutableStateOf(0) }

    // Camera permission
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Text recognizer for OCR
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // PDF Picker Launcher
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
                        Toast.makeText(context, "PDF converted to ${images.size} images", Toast.LENGTH_LONG).show()
                        shareFile(context, images.first())
                    } else {
                        Toast.makeText(context, "Failed to convert PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    // Camera Launcher for OCR
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            bitmap?.let {
                val image = InputImage.fromBitmap(it, 0)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        extractedText = visionText.text
                    }
                    .addOnFailureListener { e ->
                        extractedText = "Failed to extract text: ${e.message}"
                    }
            }
        }
    )

    // Gallery Launcher for OCR
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    showProgress = true
                    progressMessage = "Extracting text from image..."
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    bitmap?.let { bmp ->
                        val image = InputImage.fromBitmap(bmp, 0)
                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                extractedText = visionText.text
                                showProgress = false
                            }
                            .addOnFailureListener { e ->
                                extractedText = "Failed to extract text: ${e.message}"
                                showProgress = false
                            }
                    } ?: run {
                        showProgress = false
                        Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    val tools = listOf(
        ToolItem("pdf_to_jpg", "PDF to JPG", Icons.Default.PictureAsPdf, "Convert PDF pages to images"),
        ToolItem("ocr_camera", "OCR Camera", Icons.Default.CameraAlt, "Extract text from camera"),
        ToolItem("ocr_gallery", "OCR Gallery", Icons.Default.Image, "Extract text from images"),
        ToolItem("calculator", "Calculator", Icons.Default.Calculate, "Basic calculator")
    )

    if (showCalculator) {
        CalculatorScreen(onBack = { showCalculator = false })
    } else {
        ModalBottomSheet(
            onDismissRequest = onClose,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛠️ Teaching Tools",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showProgress) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                progress = if (progressTotal > 0) progressCurrent.toFloat() / progressTotal.toFloat() else 0f
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(progressMessage, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                extractedText?.let { text ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("📄 Extracted Text:", fontWeight = FontWeight.Bold)
                                IconButton(onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Text"))
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = text,
                                fontSize = 12.sp,
                                maxLines = 10,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 500.dp)
                ) {
                    items(tools) { tool ->
                        ToolCard(
                            tool = tool,
                            onClick = {
                                when (tool.id) {
                                    "pdf_to_jpg" -> {
                                        pdfPickerLauncher.launch("application/pdf")
                                    }
                                    "ocr_camera" -> {
                                        if (cameraPermissionState.status.isGranted) {
                                            cameraLauncher.launch(null)
                                        } else {
                                            cameraPermissionState.launchPermissionRequest()
                                        }
                                    }
                                    "ocr_gallery" -> {
                                        galleryLauncher.launch("image/*")
                                    }
                                    "calculator" -> {
                                        showCalculator = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolCard(tool: ToolItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                tool.icon,
                contentDescription = tool.name,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(tool.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(tool.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    val buttons = listOf(
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf("0", ".", "=", "+"),
        listOf("C", "⌫")
    )

    fun evaluateExpression(expr: String): Double {
        val tokens = expr.split(Regex("(?<=[+\\-*/])|(?=[+\\-*/])"))
        var currentResult = tokens[0].toDouble()
        var i = 1
        while (i < tokens.size) {
            val operator = tokens[i]
            val nextNum = tokens[i + 1].toDouble()
            currentResult = when (operator) {
                "+" -> currentResult + nextNum
                "-" -> currentResult - nextNum
                "*" -> currentResult * nextNum
                "/" -> currentResult / nextNum
                else -> currentResult
            }
            i += 2
        }
        return currentResult
    }

    fun calculate(expression: String): String {
        return try {
            val evalResult = evaluateExpression(expression)
            if (evalResult == evalResult.toLong().toDouble()) {
                evalResult.toLong().toString()
            } else {
                evalResult.toString()
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onBack,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🧮 Calculator", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Back")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        input.ifEmpty { "0" },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    if (result.isNotEmpty() && result != "Error") {
                        Text("= $result", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    } else if (result == "Error") {
                        Text("Error", fontSize = 18.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                buttons.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { button ->
                            CalculatorButton(
                                text = button,
                                onClick = {
                                    when (button) {
                                        "C" -> {
                                            input = ""
                                            result = ""
                                        }
                                        "⌫" -> {
                                            if (input.isNotEmpty()) {
                                                input = input.dropLast(1)
                                            }
                                        }
                                        "=" -> {
                                            if (input.isNotEmpty()) {
                                                result = calculate(input)
                                            }
                                        }
                                        else -> {
                                            input += button
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isOperator = text in listOf("+", "-", "*", "/", "=")
    val isClear = text in listOf("C", "⌫")

    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                isOperator -> MaterialTheme.colorScheme.primary
                isClear -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = when {
                isOperator -> MaterialTheme.colorScheme.onPrimary
                isClear -> MaterialTheme.colorScheme.onError
                else -> MaterialTheme.colorScheme.onSecondaryContainer
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}