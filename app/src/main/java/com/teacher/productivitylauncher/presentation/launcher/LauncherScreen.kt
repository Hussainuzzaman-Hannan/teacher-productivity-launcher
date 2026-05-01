package com.teacher.productivitylauncher.presentation.launcher

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teacher.productivitylauncher.presentation.appdrawer.AppsDrawerScreen
import com.teacher.productivitylauncher.presentation.attendance.AttendanceScreen
import com.teacher.productivitylauncher.presentation.classroutine.ClassRoutineScreen
import com.teacher.productivitylauncher.presentation.communication.DialerScreen
import com.teacher.productivitylauncher.presentation.communication.MessageScreen
import com.teacher.productivitylauncher.presentation.communication.WhatsAppScreen
import com.teacher.productivitylauncher.presentation.exams.ExamsScreen
import com.teacher.productivitylauncher.presentation.exams.ExamsViewModel
import com.teacher.productivitylauncher.presentation.exams.ExamsViewModelFactory
import com.teacher.productivitylauncher.presentation.feestracker.FeesTrackerScreen
import com.teacher.productivitylauncher.presentation.favorite.FavoriteAppsViewModel
import com.teacher.productivitylauncher.presentation.notes.NotesScreen
import com.teacher.productivitylauncher.presentation.notes.NotesViewModel
import com.teacher.productivitylauncher.presentation.notes.NotesViewModelFactory
import com.teacher.productivitylauncher.presentation.questionmaker.QuestionMakerScreen
import com.teacher.productivitylauncher.presentation.receiver.AdminReceiver
import com.teacher.productivitylauncher.presentation.settings.SettingsScreen
import com.teacher.productivitylauncher.presentation.students.StudentsPanelScreen
import com.teacher.productivitylauncher.presentation.tools.TeachingToolsScreen
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val lockScreenUtils = remember { LockScreenUtils(context) }

    LaunchedEffect(Unit) {
        if (!lockScreenUtils.isDeviceAdminActive()) {
            val intent = lockScreenUtils.requestDeviceAdmin()
            context.startActivity(intent)
        }
    }

    var lastTapTime by remember { mutableStateOf(0L) }
    val doubleTapTimeout = 300L

    var currentPage by remember { mutableStateOf(0) }
    var showAppsDrawer by remember { mutableStateOf(false) }

    var showStudentsPanel by remember { mutableStateOf(false) }
    var showTeachingTools by remember { mutableStateOf(false) }
    var showClassRoutine by remember { mutableStateOf(false) }
    var showAttendance by remember { mutableStateOf(false) }
    var showQuestionMaker by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var showExams by remember { mutableStateOf(false) }
    var showFeesTracker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    var showDialer by remember { mutableStateOf(false) }
    var showMessage by remember { mutableStateOf(false) }
    var showWhatsApp by remember { mutableStateOf(false) }

    var dragOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val currentTapTime = System.currentTimeMillis()
                        if (currentTapTime - lastTapTime < doubleTapTimeout) {
                            if (lockScreenUtils.isDeviceAdminActive()) {
                                lockScreenUtils.smartLock()
                            }
                            lastTapTime = 0L
                        } else {
                            lastTapTime = currentTapTime
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            dragOffset < -50f -> currentPage = 2
                            dragOffset > 50f -> currentPage = 1
                        }
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        when {
                            dragOffset < -50f -> showAppsDrawer = true
                            dragOffset > 50f -> showAppsDrawer = true
                        }
                        dragOffset = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    }
                )
            }
    ) {
        when (currentPage) {
            0 -> MinimalHomeScreen(
                onDialerClick = { showDialer = true },
                onMessageClick = { showMessage = true },
                onWhatsAppClick = { showWhatsApp = true },
                onNotesClick = { showNotes = true },
                onExamsClick = { showExams = true },
                onClassRoutineClick = { showClassRoutine = true }
            )
            1 -> LeftPanelScreen(
                onBack = { currentPage = 0 },
                onShowTools = { showTeachingTools = true }
            )
            2 -> RightPanelScreen(
                onBack = { currentPage = 0 },
                onShowStudents = { showStudentsPanel = true },
                onShowAttendance = { showAttendance = true },
                onShowClassRoutine = { showClassRoutine = true },
                onShowFeesTracker = { showFeesTracker = true },
                onShowSettings = { showSettings = true }
            )
        }

        if (showAppsDrawer) {
            AppsDrawerScreen(onClose = { showAppsDrawer = false })
        }

        if (showStudentsPanel) {
            StudentsPanelScreen(onClose = { showStudentsPanel = false })
        }
        if (showTeachingTools) {
            TeachingToolsScreen(onClose = { showTeachingTools = false })
        }
        if (showClassRoutine) {
            ClassRoutineScreen(onClose = { showClassRoutine = false })
        }
        if (showAttendance) {
            AttendanceScreen(onClose = { showAttendance = false })
        }
        if (showQuestionMaker) {
            QuestionMakerScreen(onClose = { showQuestionMaker = false })
        }
        if (showNotes) {
            NotesScreen(onClose = { showNotes = false })
        }
        if (showExams) {
            ExamsScreen(onClose = { showExams = false })
        }
        if (showFeesTracker) {
            FeesTrackerScreen(onClose = { showFeesTracker = false })
        }
        if (showSettings) {
            SettingsScreen(onClose = { showSettings = false })
        }
        if (showDialer) {
            DialerScreen(onClose = { showDialer = false })
        }
        if (showMessage) {
            MessageScreen(onClose = { showMessage = false })
        }
        if (showWhatsApp) {
            WhatsAppScreen(onClose = { showWhatsApp = false })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinimalHomeScreen(
    onDialerClick: () -> Unit,
    onMessageClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onNotesClick: () -> Unit,
    onExamsClick: () -> Unit,
    onClassRoutineClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clockViewModel: ClockViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context.applicationContext as android.app.Application)
    )
    val notesViewModel: NotesViewModel = viewModel(
        factory = NotesViewModelFactory(context.applicationContext as android.app.Application)
    )
    val favoriteViewModel: FavoriteAppsViewModel = viewModel()

    val favoriteApps by favoriteViewModel.favoriteApps.collectAsState()
    val packageManager = context.packageManager

    var showUnfavoriteDialog by remember { mutableStateOf(false) }
    var selectedFavoritePackage by remember { mutableStateOf<String?>(null) }
    var selectedFavoriteName by remember { mutableStateOf("") }

    val favoriteAppInfos = remember(favoriteApps) {
        favoriteApps.mapNotNull { favorite ->
            try {
                val appInfo = packageManager.getApplicationInfo(favorite.packageName, 0)
                val icon = packageManager.getApplicationIcon(appInfo)
                val name = packageManager.getApplicationLabel(appInfo).toString()
                Triple(favorite.packageName, name, icon)
            } catch (e: Exception) {
                null
            }
        }
    }

    val currentTime by clockViewModel.currentTime.collectAsState()
    val timeFormat by clockViewModel.timeFormat.collectAsState()
    val dateFormat by clockViewModel.dateFormat.collectAsState()

    val todayClasses by homeViewModel.todayClasses.collectAsState()
    val todayClassesCount by homeViewModel.todayClassesCount.collectAsState()
    val pinnedNotes by notesViewModel.pinnedNotes.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = timeFormat.format(currentTime),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = dateFormat.format(currentTime),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClassRoutineClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📚 Today's Classes", fontWeight = FontWeight.Bold)
                        if (todayClassesCount > 0) {
                            Text("$todayClassesCount classes", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    } else if (todayClasses.isEmpty()) {
                        Text(
                            "No classes scheduled for today",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        todayClasses.forEach { routine ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${routine.subjectName}",
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${routine.startTime} - ${routine.endTime}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNotesClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📝 Quick Notes", fontWeight = FontWeight.Bold)
                        Text("See all →", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (pinnedNotes.isEmpty()) {
                        Text("No notes. Tap to add", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    } else {
                        pinnedNotes.take(2).forEach { note ->
                            Text(
                                text = "• ${note.title}",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (pinnedNotes.size > 2) {
                            Text("+ ${pinnedNotes.size - 2} more", fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (favoriteAppInfos.isNotEmpty()) {
                Text(
                    text = "⭐ Favorite Apps",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    favoriteAppInfos.take(5).forEach { (packageName, name, icon) ->
                        FavoriteAppIconWithLongPress(
                            icon = icon,
                            appName = name,
                            onClick = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                                launchIntent?.let { context.startActivity(it) }
                            },
                            onLongPress = {
                                selectedFavoritePackage = packageName
                                selectedFavoriteName = name
                                showUnfavoriteDialog = true
                            }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showUnfavoriteDialog && selectedFavoritePackage != null) {
        AlertDialog(
            onDismissRequest = {
                showUnfavoriteDialog = false
                selectedFavoritePackage = null
            },
            title = { Text("Remove from Favorites") },
            text = { Text("Do you want to remove \"$selectedFavoriteName\" from favorites?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            selectedFavoritePackage?.let { packageName ->
                                favoriteViewModel.removeFromFavorites(packageName)
                                Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                            }
                            showUnfavoriteDialog = false
                            selectedFavoritePackage = null
                        }
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnfavoriteDialog = false
                    selectedFavoritePackage = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteAppIconWithLongPress(
    icon: Drawable,
    appName: String,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(60.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = rememberDrawablePainter(drawable = icon),
                contentDescription = appName,
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified
            )
        }
    }
}

@Composable
fun FavoriteAppIcon(icon: Drawable, appName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(60.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = rememberDrawablePainter(drawable = icon),
                contentDescription = appName,
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified
            )
        }
    }
}

@Composable
fun rememberDrawablePainter(drawable: Drawable): Painter {
    val bitmap = remember(drawable) {
        if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }
    return remember(bitmap) {
        BitmapPainter(bitmap.asImageBitmap())
    }
}

@Composable
fun CommunicationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeftPanelScreen(
    onBack: () -> Unit,
    onShowTools: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teaching Tools") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onShowTools, modifier = Modifier.fillMaxWidth()) {
                Text("Teaching Tools")
            }
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("PDF to JPG")
            }
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("JPG to PDF")
            }
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("OCR Scanner")
            }
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Calculator")
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "More tools coming soon",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RightPanelScreen(
    onBack: () -> Unit,
    onShowStudents: () -> Unit,
    onShowAttendance: () -> Unit,
    onShowClassRoutine: () -> Unit,
    onShowFeesTracker: () -> Unit,
    onShowSettings: () -> Unit
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context.applicationContext as android.app.Application)
    )

    val todayClasses by homeViewModel.todayClasses.collectAsState()
    val todayClassesCount by homeViewModel.todayClassesCount.collectAsState()
    val presentCount by homeViewModel.presentCount.collectAsState()
    val absentCount by homeViewModel.absentCount.collectAsState()
    val totalStudents by homeViewModel.totalStudents.collectAsState()
    val feesDueCount by homeViewModel.feesDueCount.collectAsState()
    val feesDueAmount by homeViewModel.feesDueAmount.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Students Hub") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📚 Today's Classes", fontWeight = FontWeight.Bold)
                        if (todayClassesCount > 0) {
                            Text("$todayClassesCount classes", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    } else if (todayClasses.isEmpty()) {
                        Text(
                            "No classes scheduled for today",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        todayClasses.forEach { routine ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${routine.subjectName}",
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${routine.startTime} - ${routine.endTime}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text("📊 Today's Summary", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Present", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "$presentCount",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Absent", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                Text(
                                    "$absentCount",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total", fontSize = 12.sp)
                                Text(
                                    "$totalStudents",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (feesDueCount > 0 && !isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "⚠️ Fees Due: $feesDueCount students",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Total Due: ৳${String.format("%.2f", feesDueAmount)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onShowStudents,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Students")
            }

            Button(
                onClick = onShowAttendance,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Assignment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Take Attendance")
            }

            Button(
                onClick = onShowClassRoutine,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Class Routine")
            }

            Button(
                onClick = onShowFeesTracker,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AttachMoney, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fees Tracker")
            }

            Button(
                onClick = onShowSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings")
            }
        }
    }
}

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

    fun lockWithScreenDim() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    if (Settings.System.canWrite(context)) {
                        Settings.System.putInt(
                            context.contentResolver,
                            Settings.System.SCREEN_OFF_TIMEOUT,
                            5000
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            CoroutineScope(Dispatchers.Main).launch {
                delay(5500)
                if (isDeviceAdminActive()) {
                    devicePolicyManager.lockNow()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            lockDevice()
        }
    }

    fun smartLock() {
        lockWithScreenDim()
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