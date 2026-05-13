package com.teacher.productivitylauncher.presentation.widgets

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.ClassRoutine
import com.teacher.productivitylauncher.data.local.repository.ClassRoutineRepository
import com.teacher.productivitylauncher.presentation.launcher.ClockViewModel
import com.teacher.productivitylauncher.presentation.launcher.HomeViewModelFactory
import com.teacher.productivitylauncher.presentation.notes.NotesViewModel
import com.teacher.productivitylauncher.presentation.notes.NotesViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// ── Calendar Event Data ───────────────────────────────────────
data class CalendarEvent(
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val allDay: Boolean,
    val color: Int
)

// ── Calendar ViewModel ────────────────────────────────────────
class CalendarWidgetViewModel(application: Application) : AndroidViewModel(application) {
    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    fun loadEvents(context: Context) {
        viewModelScope.launch {
            _events.value = getCalendarEvents(context)
        }
    }

    private suspend fun getCalendarEvents(context: Context): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            val events = mutableListOf<CalendarEvent>()
            try {
                val now = System.currentTimeMillis()
                val endOfWeek = now + 7 * 24 * 60 * 60 * 1000L

                val uri = CalendarContract.Events.CONTENT_URI
                val projection = arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.ALL_DAY,
                    CalendarContract.Events.CALENDAR_COLOR
                )
                val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
                val selectionArgs = arrayOf(now.toString(), endOfWeek.toString())

                context.contentResolver.query(
                    uri, projection, selection, selectionArgs,
                    "${CalendarContract.Events.DTSTART} ASC"
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        events.add(
                            CalendarEvent(
                                title = cursor.getString(1) ?: "Untitled",
                                startTime = cursor.getLong(2),
                                endTime = cursor.getLong(3),
                                allDay = cursor.getInt(4) == 1,
                                color = cursor.getInt(5)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            events.take(10)
        }
}

// ── Weather ViewModel ─────────────────────────────────────────
class WeatherWidgetViewModel(application: Application) : AndroidViewModel(application) {
    private val _weather = MutableStateFlow<WeatherData?>(null)
    val weather: StateFlow<WeatherData?> = _weather.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadWeather(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val service = WeatherService(context)
                val data = withContext(Dispatchers.IO) { service.getWeather() }
                _weather.value = data
                if (data == null) _error.value = "Location unavailable"
            } catch (e: Exception) {
                _error.value = "Weather unavailable"
            }
            _isLoading.value = false
        }
    }
}

// ── Clock Widget ──────────────────────────────────────────────
@Composable
fun ClockWidgetContent(
    config: WidgetConfig,
    modifier: Modifier = Modifier
) {
    val clockViewModel: ClockViewModel = viewModel()
    val currentTime by clockViewModel.currentTime.collectAsState()
    val timeFormat by clockViewModel.timeFormat.collectAsState()
    val dateFormat by clockViewModel.dateFormat.collectAsState()

    val height = config.size.toHeight()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                alpha = config.transparency
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        when (config.size) {
            WidgetSize.SMALL -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormat.format(currentTime),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            WidgetSize.MEDIUM -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormat.format(currentTime),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = dateFormat.format(currentTime),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            WidgetSize.LARGE -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormat.format(currentTime),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = dateFormat.format(currentTime),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val cal = Calendar.getInstance()
                        val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(currentTime)
                        Text(
                            text = dayOfWeek,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// ── Class Routine Widget ──────────────────────────────────────
@Composable
fun ClassRoutineWidgetContent(
    config: WidgetConfig,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { TeacherDatabase.getDatabase(context) }
    val repository = remember { ClassRoutineRepository(database.classRoutineDao()) }

    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val dayOfWeek = when (today) {
        Calendar.SUNDAY -> 7
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        else -> 1
    }

    val routines by repository.getRoutineByDay(dayOfWeek).collectAsState(initial = emptyList())
    val height = config.size.toHeight()

    Card(
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = config.transparency)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onTap
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📚 Today's Classes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    "${routines.size} classes",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (routines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No classes today 🎉",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                val maxItems = when (config.size) {
                    WidgetSize.SMALL -> 1
                    WidgetSize.MEDIUM -> 2
                    WidgetSize.LARGE -> 4
                }
                routines.take(maxItems).forEach { routine ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• ${routine.subjectName}",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = routine.startTime,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (routines.size > maxItems) {
                    Text(
                        "+ ${routines.size - maxItems} more",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ── Quick Notes Widget ────────────────────────────────────────
@Composable
fun QuickNotesWidgetContent(
    config: WidgetConfig,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notesViewModel: NotesViewModel = viewModel(
        factory = NotesViewModelFactory(context.applicationContext as Application)
    )
    val pinnedNotes by notesViewModel.pinnedNotes.collectAsState()
    val height = config.size.toHeight()

    Card(
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = config.transparency)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onTap
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("📝 Quick Notes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("See all →", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (pinnedNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No pinned notes",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                val maxItems = when (config.size) {
                    WidgetSize.SMALL -> 1
                    WidgetSize.MEDIUM -> 2
                    WidgetSize.LARGE -> 4
                }
                pinnedNotes.take(maxItems).forEach { note ->
                    Text(
                        text = "• ${note.title}",
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                if (pinnedNotes.size > maxItems) {
                    Text(
                        "+ ${pinnedNotes.size - maxItems} more",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ── Weather Widget ────────────────────────────────────────────
@Composable
fun WeatherWidgetContent(
    config: WidgetConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: WeatherWidgetViewModel = viewModel()
    val weather by viewModel.weather.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val height = config.size.toHeight()

    LaunchedEffect(Unit) { viewModel.loadWeather(context) }

    Card(
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = config.transparency)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.WbCloudy, contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f))
                        Text(error ?: "", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f))
                    }
                }
                weather != null -> {
                    val w = weather!!
                    when (config.size) {
                        WidgetSize.SMALL -> {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = getWeatherEmoji(w.icon),
                                    fontSize = 28.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${w.temperature.toInt()}°C",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        WidgetSize.MEDIUM -> {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = getWeatherEmoji(w.icon), fontSize = 48.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${w.temperature.toInt()}°C",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = w.cityName,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = w.description.replaceFirstChar { it.uppercase() },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        WidgetSize.LARGE -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = getWeatherEmoji(w.icon), fontSize = 48.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "${w.temperature.toInt()}°C",
                                            fontSize = 40.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(text = w.cityName, fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    WeatherDetail("💧", "${w.humidity}%", "Humidity")
                                    WeatherDetail("💨", "${w.windSpeed.toInt()} m/s", "Wind")
                                    WeatherDetail("🌡️", "${w.feelsLike.toInt()}°C", "Feels like")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherDetail(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f))
    }
}

fun getWeatherEmoji(icon: String): String = when {
    icon.startsWith("01") -> "☀️"
    icon.startsWith("02") -> "⛅"
    icon.startsWith("03") || icon.startsWith("04") -> "☁️"
    icon.startsWith("09") || icon.startsWith("10") -> "🌧️"
    icon.startsWith("11") -> "⛈️"
    icon.startsWith("13") -> "❄️"
    icon.startsWith("50") -> "🌫️"
    else -> "🌤️"
}

// ── Calendar Widget ───────────────────────────────────────────
@Composable
fun CalendarWidgetContent(
    config: WidgetConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: CalendarWidgetViewModel = viewModel()
    val events by viewModel.events.collectAsState()
    val height = config.size.toHeight()

    LaunchedEffect(Unit) { viewModel.loadEvents(context) }

    Card(
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = config.transparency)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📅 Upcoming", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                val today = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
                Text(today, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No upcoming events",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                val maxItems = when (config.size) {
                    WidgetSize.SMALL -> 1
                    WidgetSize.MEDIUM -> 2
                    WidgetSize.LARGE -> 5
                }
                events.take(maxItems).forEach { event ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!event.allDay) {
                                Text(
                                    text = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())
                                        .format(Date(event.startTime)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            } else {
                                Text(
                                    text = SimpleDateFormat("MMM d", Locale.getDefault())
                                        .format(Date(event.startTime)) + " (All day)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}