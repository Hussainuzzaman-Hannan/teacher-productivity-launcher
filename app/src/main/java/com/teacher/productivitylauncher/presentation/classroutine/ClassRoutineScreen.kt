package com.teacher.productivitylauncher.presentation.classroutine

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.PaddingValues
import android.app.Application
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.teacher.productivitylauncher.data.local.entity.ClassRoutine
import java.util.*

class ClassRoutineViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClassRoutineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClassRoutineViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassRoutineScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val viewModel: ClassRoutineViewModel = viewModel(
        factory = ClassRoutineViewModelFactory(context.applicationContext as Application)
    )

    val routines by viewModel.routines.collectAsState()
    val availableClasses by viewModel.availableClasses.collectAsState()
    val selectedClass by viewModel.selectedClass.collectAsState()
    val message by viewModel.message.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedRoutine by remember { mutableStateOf<ClassRoutine?>(null) }
    var showClassSelector by remember { mutableStateOf(false) }

    // Load routines when class changes
    LaunchedEffect(selectedClass) {
        if (selectedClass.isNotEmpty()) {
            viewModel.loadRoutinesByClass(selectedClass)
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📚 Class Routine",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Class")
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Class selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClassSelector = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (selectedClass.isEmpty()) "Select Class" else "Class: $selectedClass")
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Message
            if (message.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(message, modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Routine list by day
            if (selectedClass.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select a class to view routine", fontSize = 12.sp)
                }
            } else if (routines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No classes scheduled")
                        Text("Tap + to add a class", fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn {
                    val groupedByDay = routines.groupBy { it.dayOfWeek }
                    days.forEachIndexed { index, day ->
                        val dayRoutines = groupedByDay[index + 1] ?: emptyList()
                        if (dayRoutines.isNotEmpty()) {
                            item {
                                Text(
                                    text = day,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(dayRoutines) { routine ->
                                RoutineItem(
                                    routine = routine,
                                    onEdit = { selectedRoutine = routine },
                                    onDelete = { viewModel.deleteRoutine(it) },
                                    onToggleNotification = { viewModel.toggleNotification(it) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Class selector dialog
    if (showClassSelector) {
        AlertDialog(
            onDismissRequest = { showClassSelector = false },
            title = { Text("Select Class") },
            text = {
                Column {
                    availableClasses.forEach { className ->
                        TextButton(
                            onClick = {
                                viewModel.loadRoutinesByClass(className)
                                showClassSelector = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(className)
                        }
                    }
                    if (availableClasses.isEmpty()) {
                        Text("No classes added yet", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showClassSelector = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add/Edit dialog
    if (showAddDialog || selectedRoutine != null) {
        RoutineDialog(
            routine = selectedRoutine,
            onDismiss = {
                showAddDialog = false
                selectedRoutine = null
            },
            onSave = { subjectName, className, teacherName, dayOfWeek, startTime, endTime, roomNumber ->
                if (selectedRoutine != null) {
                    val updated = selectedRoutine!!.copy(
                        subjectName = subjectName,
                        className = className,
                        teacherName = teacherName,
                        dayOfWeek = dayOfWeek,
                        startTime = startTime,
                        endTime = endTime,
                        roomNumber = roomNumber
                    )
                    viewModel.updateRoutine(updated)
                } else {
                    viewModel.addRoutine(subjectName, className, teacherName, dayOfWeek, startTime, endTime, roomNumber)
                }
                showAddDialog = false
                selectedRoutine = null
            }
        )
    }
}

@Composable
fun RoutineItem(
    routine: ClassRoutine,
    onEdit: () -> Unit,
    onDelete: (ClassRoutine) -> Unit,
    onToggleNotification: (ClassRoutine) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(routine.subjectName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Teacher: ${routine.teacherName}", fontSize = 12.sp)
                    Text("Time: ${routine.startTime} - ${routine.endTime}", fontSize = 12.sp)
                    Text("Room: ${routine.roomNumber}", fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = { onToggleNotification(routine) }) {
                        Icon(
                            if (routine.notificationEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = "Notifications",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { onDelete(routine) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDialog(
    routine: ClassRoutine?,
    onDismiss: () -> Unit,
    onSave: (subjectName: String, className: String, teacherName: String,
             dayOfWeek: Int, startTime: String, endTime: String, roomNumber: String) -> Unit
) {
    var subjectName by remember { mutableStateOf(routine?.subjectName ?: "") }
    var className by remember { mutableStateOf(routine?.className ?: "") }
    var teacherName by remember { mutableStateOf(routine?.teacherName ?: "") }
    var selectedDay by remember { mutableStateOf(routine?.dayOfWeek ?: 1) }
    var startTime by remember { mutableStateOf(routine?.startTime ?: "09:00 AM") }
    var endTime by remember { mutableStateOf(routine?.endTime ?: "10:00 AM") }
    var roomNumber by remember { mutableStateOf(routine?.roomNumber ?: "") }

    var dayExpanded by remember { mutableStateOf(false) }
    var startTimeExpanded by remember { mutableStateOf(false) }
    var endTimeExpanded by remember { mutableStateOf(false) }
    var useManualStartTime by remember { mutableStateOf(false) }
    var useManualEndTime by remember { mutableStateOf(false) }

    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    // Time slots from 5:00 AM to 11:00 PM
    val timeSlots = listOf(
        "05:00 AM", "05:30 AM", "06:00 AM", "06:30 AM",
        "07:00 AM", "07:30 AM", "08:00 AM", "08:30 AM",
        "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM",
        "11:00 AM", "11:30 AM", "12:00 PM", "12:30 PM",
        "01:00 PM", "01:30 PM", "02:00 PM", "02:30 PM",
        "03:00 PM", "03:30 PM", "04:00 PM", "04:30 PM",
        "05:00 PM", "05:30 PM", "06:00 PM", "06:30 PM",
        "07:00 PM", "07:30 PM", "08:00 PM", "08:30 PM",
        "09:00 PM", "09:30 PM", "10:00 PM", "10:30 PM",
        "11:00 PM"
    )

    // Helper function to format time for manual input
    fun formatManualTime(input: String): String {
        var clean = input.trim().uppercase()
            .replace("AM", "")
            .replace("PM", "")
            .replace(" ", "")

        val timePattern = Regex("^([0-1]?[0-9]|2[0-3]):([0-5][0-9])$")
        if (timePattern.matches(clean)) {
            val parts = clean.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1]

            return if (hour < 12) {
                "${if (hour == 0) 12 else hour}:$minute AM"
            } else {
                "${if (hour == 12) 12 else hour - 12}:$minute PM"
            }
        }
        return input
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (routine == null) "Add Class" else "Edit Class") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Class (e.g., Class 8)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text("Teacher Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Day selection
                ExposedDropdownMenuBox(
                    expanded = dayExpanded,
                    onExpandedChange = { dayExpanded = it }
                ) {
                    OutlinedTextField(
                        value = days[selectedDay - 1],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    DropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false }
                    ) {
                        days.forEachIndexed { index, day ->
                            DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    selectedDay = index + 1
                                    dayExpanded = false
                                }
                            )
                        }
                    }
                }

                // Start Time with Manual Input Option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Start Time", fontWeight = FontWeight.Medium)
                            Row {
                                TextButton(
                                    onClick = { useManualStartTime = false },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Select",
                                        fontSize = 11.sp,
                                        color = if (!useManualStartTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                TextButton(
                                    onClick = { useManualStartTime = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Manual",
                                        fontSize = 11.sp,
                                        color = if (useManualStartTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        if (useManualStartTime) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { newValue ->
                                    startTime = newValue
                                },
                                placeholder = { Text("e.g., 09:45 AM, 10:15 AM, 2:30 PM") },
                                label = { Text("Enter Time") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Text(
                                "Format: HH:MM AM/PM (e.g., 09:45 AM, 2:30 PM)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = startTimeExpanded,
                                onExpandedChange = { startTimeExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Start Time") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startTimeExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                DropdownMenu(
                                    expanded = startTimeExpanded,
                                    onDismissRequest = { startTimeExpanded = false },
                                    modifier = Modifier.heightIn(max = 250.dp)
                                ) {
                                    timeSlots.forEach { time ->
                                        DropdownMenuItem(
                                            text = { Text(time) },
                                            onClick = {
                                                startTime = time
                                                startTimeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // End Time with Manual Input Option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("End Time", fontWeight = FontWeight.Medium)
                            Row {
                                TextButton(
                                    onClick = { useManualEndTime = false },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Select",
                                        fontSize = 11.sp,
                                        color = if (!useManualEndTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                TextButton(
                                    onClick = { useManualEndTime = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Manual",
                                        fontSize = 11.sp,
                                        color = if (useManualEndTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        if (useManualEndTime) {
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { newValue ->
                                    endTime = newValue
                                },
                                placeholder = { Text("e.g., 10:30 AM, 01:45 PM") },
                                label = { Text("Enter Time") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Text(
                                "Format: HH:MM AM/PM (e.g., 10:30 AM, 1:45 PM)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = endTimeExpanded,
                                onExpandedChange = { endTimeExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = endTime,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select End Time") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endTimeExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                DropdownMenu(
                                    expanded = endTimeExpanded,
                                    onDismissRequest = { endTimeExpanded = false },
                                    modifier = Modifier.heightIn(max = 250.dp)
                                ) {
                                    timeSlots.forEach { time ->
                                        DropdownMenuItem(
                                            text = { Text(time) },
                                            onClick = {
                                                endTime = time
                                                endTimeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Room Number") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (subjectName.isNotBlank() && className.isNotBlank() && teacherName.isNotBlank()) {
                        var finalStartTime = startTime
                        var finalEndTime = endTime

                        if (useManualStartTime) {
                            finalStartTime = formatManualTime(startTime)
                        }
                        if (useManualEndTime) {
                            finalEndTime = formatManualTime(endTime)
                        }

                        onSave(subjectName, className, teacherName, selectedDay, finalStartTime, finalEndTime, roomNumber)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}