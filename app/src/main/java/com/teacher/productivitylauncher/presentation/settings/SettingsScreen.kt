package com.teacher.productivitylauncher.presentation.settings

import androidx.compose.material3.FilterChip
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.teacher.productivitylauncher.data.local.BackupManager
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.HiddenApp
import com.teacher.productivitylauncher.presentation.appdrawer.rememberAsyncImagePainter
import com.teacher.productivitylauncher.presentation.components.AnimatedPressCard
import com.teacher.productivitylauncher.presentation.theme.AppTheme
import com.teacher.productivitylauncher.presentation.theme.ThemeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeViewModel: ThemeViewModel = viewModel()
    val backupManager = remember { BackupManager(context) }
    val database = remember { TeacherDatabase.getDatabase(context) }

    // Favorite Apps Customization State
    val homeSettingsState = rememberHomeScreenSettingsState()

    // Wallpaper ViewModel
    val wallpaperViewModel: WallpaperViewModel = viewModel()
    val wallpaperUri by wallpaperViewModel.homeWallpaperUri.collectAsState()
    val lockWallpaperUri by wallpaperViewModel.lockWallpaperUri.collectAsState()
    val blurAmount by wallpaperViewModel.blurAmount.collectAsState()
    val darkOverlay by wallpaperViewModel.darkOverlay.collectAsState()

    val appTheme by themeViewModel.appTheme.collectAsState()
    val areNotificationsEnabled by themeViewModel.areNotificationsEnabled.collectAsState()
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var showHiddenApps by remember { mutableStateOf(false) }
    var hiddenApps by remember { mutableStateOf<List<HiddenApp>>(emptyList()) }

    var showFavoriteCustomization by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            wallpaperViewModel.setHomeWallpaper(it) { success ->
                if (success) {
                    Toast.makeText(context, "Wallpaper applied successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to apply wallpaper", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(showHiddenApps) {
        if (showHiddenApps) {
            database.hiddenAppDao().getAllHidden().collect { apps ->
                hiddenApps = apps
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                scope.launch {
                    isRestoring = true
                    val success = backupManager.importDatabase(it)
                    Toast.makeText(
                        context,
                        if (success) "Restore completed successfully!" else "Restore failed. Check file format.",
                        Toast.LENGTH_LONG
                    ).show()
                    isRestoring = false
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Header ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙️ Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Progress indicators ──────────────────────────────
            if (isBackingUp) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Text("Creating backup...", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isRestoring) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Text("Restoring data...", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Theme Selector ───────────────────────────────
                item {
                    ThemeSelectorCard(
                        currentTheme = appTheme,
                        onThemeSelected = { themeViewModel.setAppTheme(it) }
                    )
                }

                // ── Wallpaper Settings ──────────────────────────────
                item {
                    SettingsButtonItem(
                        item = SettingsItem(
                            id = "wallpaper",
                            title = "🖼️ Wallpaper",
                            icon = Icons.Default.Wallpaper,
                            type = SettingsItemType.NAVIGATION,
                            onClick = { showWallpaperDialog = true }
                        )
                    )
                }

                // ── Favorite Apps Customization ───────────────────
                item {
                    SettingsButtonItem(
                        item = SettingsItem(
                            id = "favorite_customization",
                            title = "⭐ Favorite Apps Customization",
                            icon = Icons.Default.GridOn,
                            type = SettingsItemType.NAVIGATION,
                            onClick = { showFavoriteCustomization = true }
                        )
                    )
                }

                // ── Notifications ────────────────────────────────
                item {
                    SettingsSwitchItem(
                        item = SettingsItem(
                            id = "notifications",
                            title = "Class Reminders",
                            icon = Icons.Default.Notifications,
                            type = SettingsItemType.SWITCH,
                            value = areNotificationsEnabled
                        ),
                        onCheckedChange = { themeViewModel.setNotificationsEnabled(it) }
                    )
                }

                // ── Hidden Apps ──────────────────────────────────
                item {
                    SettingsButtonItem(
                        item = SettingsItem(
                            id = "hidden_apps",
                            title = "Hidden Apps",
                            icon = Icons.Default.VisibilityOff,
                            type = SettingsItemType.NAVIGATION,
                            onClick = { showHiddenApps = true }
                        )
                    )
                }

                // ── Backup ───────────────────────────────────────
                item {
                    SettingsButtonItem(
                        item = SettingsItem(
                            id = "backup",
                            title = "Backup Data",
                            icon = Icons.Default.Backup,
                            type = SettingsItemType.BUTTON,
                            onClick = {
                                scope.launch {
                                    isBackingUp = true
                                    val backupFile = backupManager.exportDatabase()
                                    if (backupFile != null) {
                                        backupManager.shareBackupFile(backupFile)
                                        Toast.makeText(context, "Backup created successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Backup failed. Please try again.", Toast.LENGTH_SHORT).show()
                                    }
                                    isBackingUp = false
                                }
                            }
                        )
                    )
                }

                // ── Restore ──────────────────────────────────────
                item {
                    SettingsButtonItem(
                        item = SettingsItem(
                            id = "restore",
                            title = "Restore Data",
                            icon = Icons.Default.Restore,
                            type = SettingsItemType.BUTTON,
                            onClick = {
                                val intent = backupManager.pickBackupFile()
                                restoreLauncher.launch(intent)
                            }
                        )
                    )
                }

                // ── Default Launcher ─────────────────────────────
                item {
                    SettingsButtonItem(
                        item = SettingsItem(
                            id = "default_launcher",
                            title = "Set as Default Launcher",
                            icon = Icons.Default.Home,
                            type = SettingsItemType.BUTTON,
                            onClick = { openDefaultLauncherSettings(context) }
                        )
                    )
                }

                // ── About ────────────────────────────────────────
                item {
                    SettingsButtonItem(
                        item = SettingsItem(
                            id = "about",
                            title = "About",
                            icon = Icons.Default.Info,
                            type = SettingsItemType.NAVIGATION,
                            onClick = { showAboutDialog = true }
                        )
                    )
                }

                // ── Version ──────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version 1.0.0",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // ── Wallpaper Dialog with Live Preview ─────────────────────
    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!wallpaperViewModel.isApplying.value) {
                    showWallpaperDialog = false
                }
            },
            title = { Text("🖼️ Wallpaper Settings") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tab Row for Home/Lock selection
                    var selectedTab by remember { mutableStateOf(0) } // 0 = Home, 1 = Lock

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            label = { Text("🏠 Home Screen") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            label = { Text("🔒 Lock Screen") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live Preview Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Show preview based on selected tab
                            val currentUri = if (selectedTab == 0) wallpaperUri else lockWallpaperUri

                            if (currentUri != null) {
                                AsyncImage(
                                    model = Uri.parse(currentUri),
                                    contentDescription = "Wallpaper Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alpha = 1f - darkOverlay
                                )

                                if (blurAmount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = blurAmount / 100f))
                                    )
                                }

                                if (darkOverlay > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = darkOverlay * 0.3f))
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Wallpaper,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            if (selectedTab == 0) "No home wallpaper selected" else "No lock wallpaper selected",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Black.copy(alpha = 0.6f)
                                )
                            ) {
                                Text(
                                    text = if (selectedTab == 0) "Home Preview" else "Lock Preview",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Choose wallpaper button
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !wallpaperViewModel.isApplying.value
                    ) {
                        if (wallpaperViewModel.isApplying.value) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Applying...")
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedTab == 0) "Choose Home Wallpaper" else "Choose Lock Wallpaper")
                        }
                    }

                    // Remove wallpaper button
                    val currentUri = if (selectedTab == 0) wallpaperUri else lockWallpaperUri
                    if (currentUri != null) {
                        Button(
                            onClick = {
                                if (selectedTab == 0) {
                                    wallpaperViewModel.removeHomeWallpaper()
                                } else {
                                    wallpaperViewModel.removeLockWallpaper()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            enabled = !wallpaperViewModel.isApplying.value
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedTab == 0) "Remove Home Wallpaper" else "Remove Lock Wallpaper")
                        }
                    }

                    HorizontalDivider()

                    // Copy to other screen button
                    if ((selectedTab == 0 && wallpaperUri != null) || (selectedTab == 1 && lockWallpaperUri != null)) {
                        Button(
                            onClick = {
                                if (selectedTab == 0 && wallpaperUri != null) {
                                    wallpaperViewModel.setLockWallpaper(Uri.parse(wallpaperUri)) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Copied to Lock Screen", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else if (selectedTab == 1 && lockWallpaperUri != null) {
                                    wallpaperViewModel.setHomeWallpaper(Uri.parse(lockWallpaperUri)) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Copied to Home Screen", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            enabled = !wallpaperViewModel.isApplying.value
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedTab == 0) "Copy to Lock Screen" else "Copy to Home Screen")
                        }
                    }

                    HorizontalDivider()

                    // Blur effect
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Blur Effect",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${blurAmount.toInt()}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = blurAmount,
                            onValueChange = { wallpaperViewModel.setBlurAmount(it) },
                            valueRange = 0f..25f,
                            steps = 10,
                            enabled = !wallpaperViewModel.isApplying.value
                        )
                    }

                    // Dark overlay
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Dark Overlay",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${((1 - darkOverlay) * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = darkOverlay,
                            onValueChange = { wallpaperViewModel.setDarkOverlay(it) },
                            valueRange = 0f..0.8f,
                            steps = 8,
                            enabled = !wallpaperViewModel.isApplying.value
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showWallpaperDialog = false },
                    enabled = !wallpaperViewModel.isApplying.value
                ) {
                    Text("Done")
                }
            }
        )
    }

    // ── Favorite Apps Customization Dialog ─────────────────────
    if (showFavoriteCustomization) {
        FavoriteAppsCustomizationDialog(
            settingsState = homeSettingsState,
            onDismiss = { showFavoriteCustomization = false }
        )
    }

    // ── Hidden Apps Dialog ───────────────────────────────────
    if (showHiddenApps) {
        AlertDialog(
            onDismissRequest = { showHiddenApps = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null)
                    Text("Hidden Apps")
                }
            },
            text = {
                if (hiddenApps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No hidden apps",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(hiddenApps) { hiddenApp ->
                            HiddenAppItem(
                                hiddenApp = hiddenApp,
                                onUnhide = {
                                    scope.launch {
                                        database.hiddenAppDao().unhideAppByPackage(hiddenApp.packageName)
                                        Toast.makeText(context, "${hiddenApp.appName} is now visible", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onOpen = {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(hiddenApp.packageName)
                                    if (launchIntent != null) context.startActivity(launchIntent)
                                    else Toast.makeText(context, "Cannot open app", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHiddenApps = false }) { Text("Close") }
            }
        )
    }

    // ── About Dialog ──────────────────────────────────────────
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("About", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Teacher Productivity Launcher",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "Version 1.0.0",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "👨‍💻 Developer",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "Zayaanify",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "📞 Contact",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("01719074004", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF25D366)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp: 01719074004", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF26A5E4)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Telegram: 01719074004", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "A minimalist launcher designed specifically for teachers to manage daily classroom activities efficiently.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// ── Favorite Apps Customization Dialog ───────────────────────
@Composable
fun FavoriteAppsCustomizationDialog(
    settingsState: HomeScreenSettingsState,
    onDismiss: () -> Unit
) {
    val settings by settingsState.settings.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Favorite Apps Customization", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Grid Columns
                Column {
                    Text(
                        "Grid Columns: ${settings.gridColumns}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = settings.gridColumns.toFloat(),
                        onValueChange = { settingsState.updateGridColumns(it.toInt()) },
                        valueRange = 2f..6f,
                        steps = 4
                    )
                    Text(
                        "কতটি কলামে অ্যাপ দেখাবে (2-6)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                HorizontalDivider()

                // Icon Size
                Column {
                    Text(
                        "Icon Size: ${settings.iconSize}dp",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = settings.iconSize.toFloat(),
                        onValueChange = { settingsState.updateIconSize(it.toInt()) },
                        valueRange = 40f..80f,
                        steps = 8
                    )
                    Text(
                        "আইকনের সাইজ (40-80dp)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                HorizontalDivider()

                // Show Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show App Labels", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = settings.showLabels,
                        onCheckedChange = { settingsState.updateShowLabels(it) }
                    )
                }

                // Label Size (if labels are shown)
                if (settings.showLabels) {
                    Column {
                        Text(
                            "Label Size: ${settings.labelSize}sp",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = settings.labelSize.toFloat(),
                            onValueChange = { settingsState.updateLabelSize(it.toInt()) },
                            valueRange = 8f..16f,
                            steps = 8
                        )
                        Text(
                            "অ্যাপের নামের টেক্সট সাইজ (8-16sp)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    HorizontalDivider()
                }

                // Horizontal Spacing
                Column {
                    Text(
                        "Horizontal Spacing: ${settings.horizontalSpacing}dp",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = settings.horizontalSpacing.toFloat(),
                        onValueChange = { settingsState.updateHorizontalSpacing(it.toInt()) },
                        valueRange = 4f..24f,
                        steps = 10
                    )
                    Text(
                        "দুইটি অ্যাপের মাঝের ফাঁকা জায়গা (4-24dp)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                HorizontalDivider()

                // Show Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show 'Favorite Apps' Title", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = settings.showFavoriteTitle,
                        onCheckedChange = { settingsState.updateShowFavoriteTitle(it) }
                    )
                }

                HorizontalDivider()

                // Preview Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Preview",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            repeat(2) { rowIndex ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(settings.horizontalSpacing.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    repeat(settings.gridColumns) { colIndex ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(settings.iconSize.dp + 16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(settings.iconSize.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                        RoundedCornerShape((settings.iconSize / 4).dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Apps,
                                                    contentDescription = null,
                                                    modifier = Modifier.size((settings.iconSize * 0.6f).dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            if (settings.showLabels) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "App",
                                                    fontSize = settings.labelSize.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowIndex == 0) {
                                    Spacer(modifier = Modifier.height(settings.verticalSpacing.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                settingsState.resetToDefault()
            }) {
                Text("Reset to Default", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

// ── Theme Selector Card ──────────────────────────────────────
@Composable
fun ThemeSelectorCard(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    val themes = listOf(
        Triple(AppTheme.LIGHT,  "☀️ Light",       "উজ্জ্বল সাদা"),
        Triple(AppTheme.DARK,   "🌙 Dark",        "গাঢ় Teal"),
        Triple(AppTheme.AMOLED, "⚫ AMOLED",      "Pure Black"),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "App Theme",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                themes.forEach { (theme, label, sub) ->
                    val selected = currentTheme == theme
                    var isPressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.93f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "theme_card_scale"
                    )
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true
                                        tryAwaitRelease()
                                        isPressed = false
                                    },
                                    onTap = { onThemeSelected(theme) }
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().let {
                            if (selected) androidx.compose.foundation.BorderStroke(
                                2.dp, MaterialTheme.colorScheme.primary
                            ) else it
                        },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (selected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(label.split(" ")[0], fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                label.split(" ")[1],
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                sub,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            if (selected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Reusable composables ─────────────────────────────────────
@Composable
fun HiddenAppItem(
    hiddenApp: HiddenApp,
    onUnhide: () -> Unit,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(hiddenApp.packageName) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(hiddenApp.packageName, 0)
            context.packageManager.getApplicationIcon(appInfo)
        } catch (e: Exception) { null }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (icon != null) {
                    Icon(
                        painter = rememberAsyncImagePainter(model = icon.toBitmap().asImageBitmap()),
                        contentDescription = hiddenApp.appName,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Unspecified
                    )
                } else {
                    Icon(
                        Icons.Default.Android,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(hiddenApp.appName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onOpen) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onUnhide) {
                    Icon(Icons.Default.Visibility, contentDescription = "Unhide",
                        tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(item: SettingsItem, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Switch(checked = item.value as Boolean, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun SettingsButtonItem(item: SettingsItem) {
    AnimatedPressCard(
        onClick = { item.onClick?.invoke() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            if (item.type == SettingsItemType.NAVIGATION) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

fun openDefaultLauncherSettings(context: android.content.Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open launcher settings", Toast.LENGTH_SHORT).show()
    }
}

enum class SettingsItemType { SWITCH, BUTTON, NAVIGATION }

data class SettingsItem(
    val id: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val type: SettingsItemType,
    val value: Any = false,
    val onClick: (() -> Unit)? = null
)