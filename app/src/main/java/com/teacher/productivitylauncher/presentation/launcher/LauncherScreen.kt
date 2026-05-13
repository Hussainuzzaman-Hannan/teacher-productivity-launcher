package com.teacher.productivitylauncher.presentation.launcher

import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.teacher.productivitylauncher.presentation.settings.WallpaperViewModel
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.Canvas
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.collectAsState
import com.teacher.productivitylauncher.presentation.settings.rememberHomeScreenSettingsState
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teacher.productivitylauncher.presentation.appdrawer.AppsDrawerScreen
import com.teacher.productivitylauncher.presentation.classroutine.ClassRoutineScreen
import com.teacher.productivitylauncher.presentation.components.AnimatedPressCard
import com.teacher.productivitylauncher.data.local.entity.FavoriteApp
import com.teacher.productivitylauncher.presentation.communication.DialerScreen
import com.teacher.productivitylauncher.presentation.communication.MessageScreen
import com.teacher.productivitylauncher.presentation.communication.WhatsAppScreen
import com.teacher.productivitylauncher.presentation.exams.ExamsScreen
import com.teacher.productivitylauncher.presentation.favorite.FavoriteAppsViewModel
import com.teacher.productivitylauncher.presentation.notes.NotesScreen
import com.teacher.productivitylauncher.presentation.notes.NotesViewModel
import com.teacher.productivitylauncher.presentation.notes.NotesViewModelFactory
import com.teacher.productivitylauncher.presentation.questionmaker.QuestionMakerScreen
import com.teacher.productivitylauncher.presentation.receiver.AdminReceiver
import com.teacher.productivitylauncher.presentation.settings.SettingsScreen
import com.teacher.productivitylauncher.presentation.settings.HomeScreenSettings
import com.teacher.productivitylauncher.presentation.tools.TeachingToolsScreen
import com.teacher.productivitylauncher.presentation.widgets.WidgetScreen
import com.teacher.productivitylauncher.presentation.widgets.WidgetConfig
import com.teacher.productivitylauncher.presentation.widgets.WidgetType
import com.teacher.productivitylauncher.presentation.widgets.WidgetViewModel
import com.teacher.productivitylauncher.presentation.widgets.ClassRoutineWidgetContent
import com.teacher.productivitylauncher.presentation.widgets.QuickNotesWidgetContent
import com.teacher.productivitylauncher.presentation.widgets.WeatherWidgetContent
import com.teacher.productivitylauncher.presentation.widgets.CalendarWidgetContent
import com.teacher.productivitylauncher.presentation.widgets.lifeTipsQuotes
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val lockScreenUtils = remember { LockScreenUtils(context) }

    val homeSettingsState = rememberHomeScreenSettingsState()
    val settings by homeSettingsState.settings.collectAsState()

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context.applicationContext as android.app.Application)
    )

    LaunchedEffect(Unit) {
        if (!lockScreenUtils.isDeviceAdminActive()) {
            val intent = lockScreenUtils.requestDeviceAdmin()
            context.startActivity(intent)
        }
        ClassReminderService.createNotificationChannel(context)
        homeViewModel.initReminders(context)
    }

    var lastTapTime by remember { mutableStateOf(0L) }
    val doubleTapTimeout = 300L

    var currentPage by remember { mutableStateOf(0) }
    var showAppsDrawer by remember { mutableStateOf(false) }

    var showTeachingTools by remember { mutableStateOf(false) }
    var showClassRoutine by remember { mutableStateOf(false) }
    var showQuestionMaker by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var showExams by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showWidgets by remember { mutableStateOf(false) }

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
                onClassRoutineClick = { showClassRoutine = true },
                settings = settings
            )
            1 -> LeftPanelScreen(
                onBack = { currentPage = 0 },
                onShowTools = { showTeachingTools = true }
            )
            2 -> RightPanelScreen(
                onBack = { currentPage = 0 },
                onShowClassRoutine = { showClassRoutine = true },
                onShowSettings = { showSettings = true },
                onShowWidgets = { showWidgets = true }
            )
        }

        if (showAppsDrawer) {
            AppsDrawerScreen(onClose = { showAppsDrawer = false })
        }
        if (showTeachingTools) {
            TeachingToolsScreen(onClose = { showTeachingTools = false })
        }
        if (showClassRoutine) {
            ClassRoutineScreen(onClose = { showClassRoutine = false })
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
        if (showSettings) {
            SettingsScreen(onClose = { showSettings = false })
        }
        if (showWidgets) {
            WidgetScreen(
                onClose = { showWidgets = false },
                onClassRoutineClick = { showClassRoutine = true },
                onNotesClick = { showNotes = true }
            )
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

data class FavoriteAppInfo(
    val app: FavoriteApp,
    val name: String,
    val icon: android.graphics.drawable.Drawable
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinimalHomeScreen(
    onDialerClick: () -> Unit,
    onMessageClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onNotesClick: () -> Unit,
    onExamsClick: () -> Unit,
    onClassRoutineClick: () -> Unit,
    settings: HomeScreenSettings = HomeScreenSettings.default()
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
    val widgetViewModel: WidgetViewModel = viewModel()

    val wallpaperViewModel: WallpaperViewModel = viewModel()
    val wallpaperUri by wallpaperViewModel.homeWallpaperUri.collectAsState()
    val blurAmount by wallpaperViewModel.blurAmount.collectAsState()
    val darkOverlay by wallpaperViewModel.darkOverlay.collectAsState()

    val widgets by widgetViewModel.widgets.collectAsState()
    val homeWidgets = remember(widgets) {
        widgets.filter { it.showOnHome }.sortedBy { it.order }
    }

    val favoriteApps by favoriteViewModel.favoriteApps.collectAsState()
    val packageManager = context.packageManager

    var showUnfavoriteDialog by remember { mutableStateOf(false) }
    var selectedFavoritePackage by remember { mutableStateOf<String?>(null) }
    var selectedFavoriteName by remember { mutableStateOf("") }
    var showRearrangeDialog by remember { mutableStateOf(false) }

    val favoriteAppInfos = remember(favoriteApps) {
        favoriteApps.mapNotNull { favorite ->
            try {
                val appInfo = packageManager.getApplicationInfo(favorite.packageName, 0)
                val icon = packageManager.getApplicationIcon(appInfo)
                val name = packageManager.getApplicationLabel(appInfo).toString()
                FavoriteAppInfo(favorite, name, icon)
            } catch (e: Exception) { null }
        }
    }

    var rearrangeList by remember(favoriteAppInfos) {
        mutableStateOf(favoriteAppInfos.toMutableList())
    }

    val currentTime by clockViewModel.currentTime.collectAsState()
    val timeFormat by clockViewModel.timeFormat.collectAsState()
    val dateFormat by clockViewModel.dateFormat.collectAsState()
    val clockStyle by clockViewModel.clockStyle.collectAsState()

    val todayClasses by homeViewModel.todayClasses.collectAsState()
    val todayClassesCount by homeViewModel.todayClassesCount.collectAsState()
    val pinnedNotes by notesViewModel.pinnedNotes.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()

    val classStatuses by homeViewModel.classStatuses.collectAsState()
    val completedClassesCount by homeViewModel.completedClassesCount.collectAsState()

    LaunchedEffect(currentTime) {
        homeViewModel.updateClassStatuses(currentTime)
    }

    val dayName = remember(currentTime) {
        SimpleDateFormat("EEEE", Locale.getDefault()).format(currentTime)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Wallpaper Background
        if (wallpaperUri != null) {
            AsyncImage(
                model = android.net.Uri.parse(wallpaperUri),
                contentDescription = "Wallpaper",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 1f - darkOverlay
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
            )
        }

        // Blur overlay - Using Box with background (Canvas error fixed)
        if (blurAmount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = blurAmount / 100f))
            )
        }

        // Dark overlay - Using Box with background (Canvas error fixed)
        if (darkOverlay > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = darkOverlay * 0.3f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            ClockDisplay(
                currentTime = currentTime,
                timeFormat = timeFormat,
                dateFormat = dateFormat,
                clockStyle = clockStyle,
                onLongPress = { clockViewModel.nextClockStyle() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            HeroGreetingSection(
                todayClassesCount = todayClassesCount - completedClassesCount,
                totalClassesCount = todayClassesCount,
                completedClassesCount = completedClassesCount,
                currentTime = currentTime
            )

            Spacer(modifier = Modifier.height(16.dp))

            homeWidgets.forEach { widget ->
                when (widget.type) {
                    WidgetType.CLOCK -> { }
                    WidgetType.CLASS_ROUTINE -> {
                        ClassRoutineWidgetContent(
                            config = widget,
                            onTap = onClassRoutineClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    WidgetType.QUICK_NOTES -> {
                        QuickNotesWidgetContent(
                            config = widget,
                            onTap = onNotesClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    WidgetType.WEATHER -> {
                        WeatherWidgetContent(
                            config = widget,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    WidgetType.CALENDAR -> {
                        CalendarWidgetContent(
                            config = widget,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (widget.type != WidgetType.CLOCK) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            if (favoriteAppInfos.isNotEmpty()) {
                if (settings.showFavoriteTitle) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⭐ Favorite Apps",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { showRearrangeDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Rearrange",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showRearrangeDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Rearrange",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                FavoriteAppsGrid(
                    apps = favoriteAppInfos,
                    settings = settings,
                    onAppClick = { info ->
                        val launchIntent = context.packageManager
                            .getLaunchIntentForPackage(info.app.packageName)
                        launchIntent?.let { context.startActivity(it) }
                    },
                    onAppLongPress = { info ->
                        selectedFavoritePackage = info.app.packageName
                        selectedFavoriteName = info.name
                        showUnfavoriteDialog = true
                    }
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showUnfavoriteDialog && selectedFavoritePackage != null) {
        AlertDialog(
            onDismissRequest = {
                showUnfavoriteDialog = false
                selectedFavoritePackage = null
            },
            title = { Text("Remove from Favorites") },
            text = { Text("\"$selectedFavoriteName\" কে favorites থেকে সরাবেন?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pkgToRemove = selectedFavoritePackage
                        showUnfavoriteDialog = false
                        selectedFavoritePackage = null
                        if (pkgToRemove != null) {
                            scope.launch {
                                favoriteViewModel.removeFromFavorites(pkgToRemove)
                                Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                            }
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

    if (showRearrangeDialog) {
        AlertDialog(
            onDismissRequest = { showRearrangeDialog = false },
            title = { Text("Rearrange Apps") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "↑ ↓ বাটন দিয়ে অ্যাপের অর্ডার পরিবর্তন করুন",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    rearrangeList.forEachIndexed { index, info ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        painter = rememberDrawablePainter(drawable = info.icon),
                                        contentDescription = info.name,
                                        modifier = Modifier.size(32.dp),
                                        tint = Color.Unspecified
                                    )
                                    Text(info.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val newList = rearrangeList.toMutableList()
                                                val temp = newList[index]
                                                newList[index] = newList[index - 1]
                                                newList[index - 1] = temp
                                                rearrangeList = newList
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move Up",
                                            modifier = Modifier.size(20.dp),
                                            tint = if (index > 0) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < rearrangeList.size - 1) {
                                                val newList = rearrangeList.toMutableList()
                                                val temp = newList[index]
                                                newList[index] = newList[index + 1]
                                                newList[index + 1] = temp
                                                rearrangeList = newList
                                            }
                                        },
                                        enabled = index < rearrangeList.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move Down",
                                            modifier = Modifier.size(20.dp),
                                            tint = if (index < rearrangeList.size - 1) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        favoriteViewModel.reorder(rearrangeList.map { it.app })
                        showRearrangeDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRearrangeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun HeroGreetingSection(
    todayClassesCount: Int,
    totalClassesCount: Int,
    completedClassesCount: Int,
    currentTime: Date
) {
    val hour = remember(currentTime) {
        Calendar.getInstance().apply { time = currentTime }.get(Calendar.HOUR_OF_DAY)
    }

    val (greeting, emoji, subText) = remember(hour) {
        when (hour) {
            in 5..11 -> Triple("Good Morning", "🌅", "Have a beautiful day ahead")
            in 12..13 -> Triple("Good Afternoon", "☀️", "Take a moment to relax")
            in 14..16 -> Triple("Good Afternoon", "🌤️", "Keep going, you've got this")
            in 17..20 -> Triple("Good Evening", "🌇", "How was your day today?")
            else -> Triple("Good Night", "🌙", "Rest well, Sir")
        }
    }

    val classText = when {
        totalClassesCount == 0 -> "No classes today 🎉"
        todayClassesCount == 0 && completedClassesCount == totalClassesCount && totalClassesCount > 0 ->
            "✅ All classes done! Time to rest"
        todayClassesCount == 0 && totalClassesCount > 0 -> "🎉 No more classes today!"
        todayClassesCount == 1 -> "1 more class remaining today"
        else -> "$todayClassesCount more classes remaining today"
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                        Text(
                            text = greeting,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (todayClassesCount == 0 && totalClassesCount > 0)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Text(
                        text = classText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClockDisplay(
    currentTime: Date,
    timeFormat: SimpleDateFormat,
    dateFormat: SimpleDateFormat,
    clockStyle: Int,
    onLongPress: () -> Unit
) {
    val calendar = remember(currentTime) {
        Calendar.getInstance().apply { time = currentTime }
    }

    val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
    val hour = calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)
    val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

    val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(currentTime)
    val fullDate = dateFormat.format(currentTime)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            )
    ) {
        when (clockStyle) {
            0 -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = timeFormat.format(currentTime),
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = fullDate,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            1 -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "%02d:%02d".format(hour, minute),
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = ":%02d".format(second),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = amPm,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = dayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            2 -> {
                val primaryColor = MaterialTheme.colorScheme.primary
                val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Canvas(modifier = Modifier.size(200.dp)) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val radius = size.width / 2

                            drawCircle(color = surfaceColor, radius = radius, center = center)
                            drawCircle(
                                color = primaryColor, radius = radius, center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                            )

                            for (i in 1..12) {
                                val angle = Math.toRadians((i * 30 - 90).toDouble())
                                val startR = radius * 0.85f
                                val endR = radius * 0.95f

                                val startPoint = Offset(
                                    center.x + (startR * cos(angle)).toFloat(),
                                    center.y + (startR * sin(angle)).toFloat()
                                )
                                val endPoint = Offset(
                                    center.x + (endR * cos(angle)).toFloat(),
                                    center.y + (endR * sin(angle)).toFloat()
                                )

                                drawLine(
                                    color = onSurfaceColor,
                                    start = startPoint,
                                    end = endPoint,
                                    strokeWidth = if (i % 3 == 0) 5f else 2f
                                )
                            }

                            val hourAngle = Math.toRadians(((hour24 % 12) * 30 + minute * 0.5 - 90).toDouble())
                            drawLine(
                                color = onSurfaceColor,
                                start = center,
                                end = Offset(
                                    center.x + (radius * 0.45f * cos(hourAngle)).toFloat(),
                                    center.y + (radius * 0.45f * sin(hourAngle)).toFloat()
                                ),
                                strokeWidth = 10f,
                                cap = StrokeCap.Round
                            )

                            val minuteAngle = Math.toRadians((minute * 6 - 90).toDouble())
                            drawLine(
                                color = onSurfaceColor,
                                start = center,
                                end = Offset(
                                    center.x + (radius * 0.65f * cos(minuteAngle)).toFloat(),
                                    center.y + (radius * 0.65f * sin(minuteAngle)).toFloat()
                                ),
                                strokeWidth = 6f,
                                cap = StrokeCap.Round
                            )

                            val secondAngle = Math.toRadians((second * 6 - 90).toDouble())
                            drawLine(
                                color = primaryColor,
                                start = center,
                                end = Offset(
                                    center.x + (radius * 0.75f * cos(secondAngle)).toFloat(),
                                    center.y + (radius * 0.75f * sin(secondAngle)).toFloat()
                                ),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )

                            drawCircle(color = primaryColor, radius = 8f, center = center)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = fullDate,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            3 -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = timeFormat.format(currentTime).uppercase(),
                                fontSize = 52.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 4.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = fullDate,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            4 -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A2E)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%02d".format(hour),
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF88),
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "HOURS",
                                fontSize = 9.sp,
                                color = Color(0xFF00FF88).copy(alpha = 0.6f),
                                letterSpacing = 1.sp                            )
                        }

                        Text(
                            text = ":",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF3366),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%02d".format(minute),
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF3366),
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "MINUTES",
                                fontSize = 9.sp,
                                color = Color(0xFFFF3366).copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = amPm,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFFF00)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = SimpleDateFormat("dd/MM", Locale.getDefault()).format(currentTime),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (clockStyle == index) 8.dp else 5.dp)
                        .background(
                            color = if (clockStyle == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun FavoriteAppsGrid(
    apps: List<FavoriteAppInfo>,
    settings: HomeScreenSettings,
    onAppClick: (FavoriteAppInfo) -> Unit,
    onAppLongPress: (FavoriteAppInfo) -> Unit
) {
    val groupedApps = apps.chunked(settings.gridColumns)
    Column(verticalArrangement = Arrangement.spacedBy(settings.verticalSpacing.dp)) {
        groupedApps.forEach { rowApps ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(settings.horizontalSpacing.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                rowApps.forEach { info ->
                    FavoriteAppIconWithLongPress(
                        icon = info.icon,
                        appName = info.name,
                        iconSize = settings.iconSize.dp,
                        showLabel = settings.showLabels,
                        labelSize = settings.labelSize.sp,
                        onClick = { onAppClick(info) },
                        onLongPress = { onAppLongPress(info) }
                    )
                }
                repeat(settings.gridColumns - rowApps.size) {
                    Spacer(modifier = Modifier.width(settings.iconSize.dp + 16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteAppIconWithLongPress(
    icon: Drawable,
    appName: String,
    iconSize: androidx.compose.ui.unit.Dp = 60.dp,
    showLabel: Boolean = true,
    labelSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(iconSize + 16.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(iconSize),
            shape = RoundedCornerShape(iconSize.value / 4),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Icon(
                painter = rememberDrawablePainter(drawable = icon),
                contentDescription = appName,
                modifier = Modifier.fillMaxSize(),
                tint = Color.Unspecified
            )
        }
        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = appName, fontSize = labelSize, maxLines = 1,
                overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
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
            val bmp = android.graphics.Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bmp)  // ← Full path ব্যবহার করুন
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }
    }
    return remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeftPanelScreen(
    onBack: () -> Unit,
    onShowTools: () -> Unit
) {
    var showToolsPanel by remember { mutableStateOf(false) }

    // When back button is pressed, close the drawer
    LaunchedEffect(Unit) {
        showToolsPanel = true
    }

    ModalNavigationDrawer(
        drawerState = rememberDrawerState(initialValue = DrawerValue.Open),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxSize(),  // 🔥 Full screen
                drawerContainerColor = MaterialTheme.colorScheme.background
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛠️ Teaching Tools",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Tools content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Question Maker
                    TeachingToolItem(
                        icon = Icons.Default.Edit,
                        title = "Question Maker",
                        description = "Create exam questions",
                        onClick = onShowTools
                    )

                    // Exams
                    TeachingToolItem(
                        icon = Icons.Default.DateRange,
                        title = "Exams",
                        description = "Manage exams",
                        onClick = { /* Navigate */ }
                    )

                    // Class Routine
                    TeachingToolItem(
                        icon = Icons.Default.Schedule,
                        title = "Class Routine",
                        description = "View class schedule",
                        onClick = { /* Navigate */ }
                    )

                    // Notes
                    TeachingToolItem(
                        icon = Icons.Default.Note,
                        title = "Notes",
                        description = "Quick notes",
                        onClick = { /* Navigate */ }
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        // Empty content because drawer is always open
        Box(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun TeachingToolItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
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
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RightPanelScreen(
    onBack: () -> Unit,
    onShowClassRoutine: () -> Unit,
    onShowSettings: () -> Unit,
    onShowWidgets: () -> Unit
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context.applicationContext as android.app.Application)
    )

    // 🔥 WidgetViewModel for persistent quote index
    val widgetViewModel: WidgetViewModel = viewModel()

    // 🔥 Load saved index from Storage
    val savedIndex = widgetViewModel.quoteIndex.value
    var currentIndex by remember { mutableStateOf(savedIndex) }
    val quotes = lifeTipsQuotes

    val todayClasses by homeViewModel.todayClasses.collectAsState()
    val todayClassesCount by homeViewModel.todayClassesCount.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()

    // 🔥 প্রতি 5 সেকেন্ডে পরিবর্তন হবে (30 থেকে 5 করা হয়েছে)
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // 5 seconds delay
            val newIndex = (currentIndex + 1) % quotes.size
            currentIndex = newIndex
            widgetViewModel.updateQuoteIndex(newIndex)
        }
    }

    LaunchedEffect(Unit) { homeViewModel.refreshData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Access") },
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
            // Today's Classes Card
            AnimatedPressCard(
                onClick = onShowClassRoutine,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📚 Today's Classes", fontWeight = FontWeight.Bold)
                        if (todayClassesCount > 0) {
                            Text("$todayClassesCount classes", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    } else if (todayClasses.isEmpty()) {
                        Text("No classes scheduled for today", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    } else {
                        todayClasses.forEach { routine ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${routine.subjectName}", fontSize = 13.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${routine.startTime} - ${routine.endTime}",
                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            Button(onClick = onShowWidgets, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Widgets, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Widgets")
            }

            Button(onClick = onShowSettings, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings")
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(text = "📺", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Life Tips",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "📺", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 🔥 Quote text shows current index (which saved)
                    Text(
                        text = quotes[currentIndex],
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress indicator
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

class LockScreenUtils(private val context: Context) {

    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent: ComponentName = ComponentName(context, AdminReceiver::class.java)

    fun isDeviceAdminActive(): Boolean = devicePolicyManager.isAdminActive(adminComponent)

    fun requestDeviceAdmin(): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for screen lock functionality")
        }
    }

    fun lockDevice() {
        if (isDeviceAdminActive()) devicePolicyManager.lockNow()
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
                } catch (e: Exception) { e.printStackTrace() }
            }
            CoroutineScope(Dispatchers.Main).launch {
                delay(5500)
                if (isDeviceAdminActive()) devicePolicyManager.lockNow()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            lockDevice()
        }
    }

    fun smartLock() { lockWithScreenDim() }

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