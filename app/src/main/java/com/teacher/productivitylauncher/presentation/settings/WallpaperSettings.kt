package com.teacher.productivitylauncher.presentation.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@Composable
fun WallpaperSettingsScreen(
    onClose: () -> Unit,
    viewModel: WallpaperViewModel = viewModel()
) {
    val context = LocalContext.current

    val homeWallpaperUri by viewModel.homeWallpaperUri.collectAsState()
    val lockWallpaperUri by viewModel.lockWallpaperUri.collectAsState()
    val blurAmount by viewModel.blurAmount.collectAsState()
    val darkOverlay by viewModel.darkOverlay.collectAsState()
    val isApplying by viewModel.isApplying.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val currentUri = if (selectedTab == 0) homeWallpaperUri else lockWallpaperUri

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (selectedTab == 0) {
                viewModel.setHomeWallpaper(it) { success ->
                    Toast.makeText(
                        context,
                        if (success) "Home wallpaper applied! ✅" else "Failed ❌",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                viewModel.setLockWallpaper(it) { success ->
                    Toast.makeText(
                        context,
                        if (success) "Lock wallpaper applied! ✅" else "Failed ❌",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isApplying) onClose() },
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

            // ── Background / Preview ──────────────────────────────
            if (currentUri != null) {
                AsyncImage(
                    model = Uri.parse(currentUri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                ),
                                startY = 300f
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
            }

            // ── Top Bar ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (!isApplying) onClose() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Text(
                    text = "Wallpaper",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TabChip(text = "🏠", selected = selectedTab == 0, onClick = { selectedTab = 0 })
                    TabChip(text = "🔒", selected = selectedTab == 1, onClick = { selectedTab = 1 })
                }
            }

            // ── Bottom Controls ───────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (selectedTab == 0) "🏠 Home Screen" else "🔒 Lock Screen",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f)
                )

                SliderRow(
                    label = "Blur",
                    value = blurAmount,
                    onValueChange = { viewModel.setBlurAmount(it) },
                    valueRange = 0f..25f,
                    displayValue = "${blurAmount.toInt()}%",
                    enabled = currentUri != null && !isApplying
                )

                SliderRow(
                    label = "Dim",
                    value = darkOverlay,
                    onValueChange = { viewModel.setDarkOverlay(it) },
                    valueRange = 0f..0.8f,
                    displayValue = "${((darkOverlay / 0.8f) * 100).toInt()}%",
                    enabled = currentUri != null && !isApplying
                )

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Choose Photo
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        enabled = !isApplying,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Applying...", fontSize = 13.sp)
                        } else {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Photo", fontSize = 13.sp)
                        }
                    }

                    // Copy to other screen
                    if (currentUri != null) {
                        OutlinedButton(
                            onClick = {
                                if (selectedTab == 0 && homeWallpaperUri != null) {
                                    viewModel.setLockWallpaper(Uri.parse(homeWallpaperUri!!)) { success ->
                                        if (success) Toast.makeText(
                                            context, "Copied to Lock Screen ✅", Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else if (selectedTab == 1 && lockWallpaperUri != null) {
                                    viewModel.setHomeWallpaper(Uri.parse(lockWallpaperUri!!)) { success ->
                                        if (success) Toast.makeText(
                                            context, "Copied to Home Screen ✅", Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isApplying,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color.White.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (selectedTab == 0) "→ Lock" else "→ Home",
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Remove Button
                if (currentUri != null) {
                    TextButton(
                        onClick = {
                            if (selectedTab == 0) viewModel.removeHomeWallpaper()
                            else viewModel.removeLockWallpaper()
                        },
                        enabled = !isApplying,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Remove wallpaper",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) Color.White.copy(alpha = 0.25f) else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 16.sp)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.width(36.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
        Text(
            text = displayValue,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.width(36.dp)
        )
    }
}