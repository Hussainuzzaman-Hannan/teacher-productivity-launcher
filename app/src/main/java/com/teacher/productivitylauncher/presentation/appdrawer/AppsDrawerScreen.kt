package com.teacher.productivitylauncher.presentation.appdrawer

import androidx.compose.foundation.background
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teacher.productivitylauncher.data.local.AppInfo
import com.teacher.productivitylauncher.presentation.favorite.FavoriteAppsViewModel
import com.teacher.productivitylauncher.presentation.favorite.FavoriteAppsViewModelFactory
import kotlinx.coroutines.launch

class AppsDrawerViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppsDrawerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppsDrawerViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun rememberAsyncImagePainter(model: androidx.compose.ui.graphics.ImageBitmap): Painter {
    return remember(model) {
        BitmapPainter(model)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppsDrawerScreen(
    onClose: () -> Unit,
    viewModel: AppsDrawerViewModel = viewModel(
        factory = AppsDrawerViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoriteViewModel: FavoriteAppsViewModel = viewModel(
        factory = FavoriteAppsViewModelFactory(LocalContext.current.applicationContext as Application)
    )

    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameApp by remember { mutableStateOf<AppInfo?>(null) }
    var newAppName by remember { mutableStateOf("") }

    // FULL SCREEN DIALOG
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Apps",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row {
                        // Refresh button
                        IconButton(onClick = { viewModel.refreshApps() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = if (isRefreshing) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Loading indicator
                if (isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Apps Grid - Full Height with Performance Optimizations
                if (filteredApps.isEmpty() && !isRefreshing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No apps found",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            count = filteredApps.size,
                            key = { index -> filteredApps[index].packageName }
                        ) { index ->
                            val app = filteredApps[index]
                            AppIconCard(
                                app = app,
                                onAppClick = { viewModel.openApp(app) },
                                onAppLongPress = {
                                    selectedApp = app
                                    showMenu = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog && renameApp != null) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                renameApp = null
                newAppName = ""
            },
            title = { Text("Rename ${renameApp!!.name}") },
            text = {
                OutlinedTextField(
                    value = newAppName,
                    onValueChange = { newAppName = it },
                    label = { Text("New name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val appToRename = renameApp
                        if (newAppName.isNotBlank() && appToRename != null) {
                            val nameToSave = newAppName
                            showRenameDialog = false
                            renameApp = null
                            newAppName = ""
                            scope.launch {
                                viewModel.renameApp(appToRename.packageName, nameToSave, appToRename.name)
                                Toast.makeText(context, "App renamed to $nameToSave", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showRenameDialog = false
                            renameApp = null
                            newAppName = ""
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    renameApp = null
                    newAppName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Context Menu Dialog
    if (showMenu && selectedApp != null) {
        val app = selectedApp!!

        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = rememberAsyncImagePainter(
                            model = app.icon.toBitmap().asImageBitmap()
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = androidx.compose.ui.graphics.Color.Unspecified
                    )
                    Text(app.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MenuItem(icon = Icons.Default.Favorite, text = "Add to favorites") {
                        scope.launch {
                            val success = favoriteViewModel.addToFavorites(app.packageName, app.name)
                            if (success) {
                                Toast.makeText(context, "⭐ Added to favorites", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Maximum 5 favorites only!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showMenu = false
                    }
                    MenuItem(icon = Icons.Default.Block, text = "Block") {
                        scope.launch {
                            viewModel.blockApp(app.packageName, app.name)
                            Toast.makeText(context, "✋ App blocked", Toast.LENGTH_SHORT).show()
                        }
                        showMenu = false
                    }
                    MenuItem(icon = Icons.Default.Edit, text = "Rename") {
                        renameApp = app
                        newAppName = app.name
                        showRenameDialog = true
                        showMenu = false
                    }
                    MenuItem(icon = Icons.Default.VisibilityOff, text = "Hide") {
                        scope.launch {
                            viewModel.hideApp(app.packageName, app.name)
                            Toast.makeText(context, "👁️ App hidden", Toast.LENGTH_SHORT).show()
                        }
                        showMenu = false
                    }
                    MenuItem(icon = Icons.Default.Folder, text = "Move to folder") {
                        Toast.makeText(context, "Coming soon: Folder", Toast.LENGTH_SHORT).show()
                        showMenu = false
                    }
                    HorizontalDivider()
                    MenuItem(icon = Icons.Default.Delete, text = "Uninstall", isDestructive = true) {
                        openAppInfo(context, app.packageName)
                        showMenu = false
                    }
                    MenuItem(icon = Icons.Default.Info, text = "App info") {
                        openAppInfo(context, app.packageName)
                        showMenu = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenu = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconCard(
    app: AppInfo,
    onAppClick: () -> Unit,
    onAppLongPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .combinedClickable(
                onClick = onAppClick,
                onLongClick = onAppLongPress
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                painter = rememberAsyncImagePainter(
                    model = app.icon.toBitmap().asImageBitmap()
                ),
                contentDescription = app.name,
                modifier = Modifier.fillMaxSize(),
                tint = androidx.compose.ui.graphics.Color.Unspecified
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = app.name,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text,
            fontSize = 14.sp,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

fun openAppInfo(context: android.content.Context, packageName: String) {
    try {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open app info", Toast.LENGTH_SHORT).show()
    }
}