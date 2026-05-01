package com.teacher.productivitylauncher.presentation.feestracker

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
import com.teacher.productivitylauncher.data.local.entity.FeesRecord
import com.teacher.productivitylauncher.presentation.students.StudentViewModel
import com.teacher.productivitylauncher.presentation.students.StudentViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class FeesViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeesViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeesTrackerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val feesViewModel: FeesViewModel = viewModel(
        factory = FeesViewModelFactory(context.applicationContext as Application)
    )
    val studentViewModel: StudentViewModel = viewModel(
        factory = StudentViewModelFactory(context.applicationContext as Application)
    )

    val feesRecords by feesViewModel.feesRecords.collectAsState()
    val unpaidFees by feesViewModel.unpaidFees.collectAsState()
    val totalPaid by feesViewModel.totalPaid.collectAsState()
    val totalDue by feesViewModel.totalDue.collectAsState()
    val unpaidCount by feesViewModel.unpaidCount.collectAsState()
    val selectedMonth by feesViewModel.selectedMonth.collectAsState()
    val message by feesViewModel.message.collectAsState()
    val students by studentViewModel.students.collectAsState()

    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showMonthSelector by remember { mutableStateOf(false) }
    var selectedStudentId by remember { mutableStateOf(0) }
    var selectedStudentName by remember { mutableStateOf("") }
    var selectedStudentClass by remember { mutableStateOf("") }

    val months = feesViewModel.getAvailableMonths()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

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
                    text = "💰 Fees Tracker",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    IconButton(onClick = { showAddPaymentDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Payment")
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month Selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMonthSelector = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Month: $selectedMonth", fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.DateRange, contentDescription = "Change Month")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Total Paid",
                    amount = totalPaid,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Total Due",
                    amount = totalDue,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⚠️ Unpaid Students", fontWeight = FontWeight.Medium)
                    Text("$unpaidCount students", color = MaterialTheme.colorScheme.error)
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

            // Fees Records List
            if (feesRecords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No fee records for $selectedMonth")
                        Text("Tap + to add payment", fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn {
                    items(feesRecords) { record ->
                        FeesRecordItem(
                            record = record,
                            dateFormat = dateFormat,
                            onDelete = { feesViewModel.deleteFeesRecord(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Month Selector Dialog
    if (showMonthSelector) {
        AlertDialog(
            onDismissRequest = { showMonthSelector = false },
            title = { Text("Select Month") },
            text = {
                Column {
                    if (months.isEmpty()) {
                        Text("No records yet", fontSize = 12.sp)
                    } else {
                        months.forEach { month ->
                            TextButton(
                                onClick = {
                                    feesViewModel.changeMonth(month)
                                    showMonthSelector = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(month)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthSelector = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Payment Dialog
    if (showAddPaymentDialog) {
        AddPaymentDialog(
            students = students,
            onDismiss = { showAddPaymentDialog = false },
            onAddPayment = { studentId, studentName, className, amount, method, remarks ->
                feesViewModel.addFeesPayment(studentId, studentName, className, amount.toDoubleOrNull() ?: 0.0, method, remarks)
                showAddPaymentDialog = false
            }
        )
    }
}

@Composable
fun SummaryCard(title: String, amount: Double, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp)
            Text(
                "৳${String.format("%.2f", amount)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun FeesRecordItem(record: FeesRecord, dateFormat: SimpleDateFormat, onDelete: (FeesRecord) -> Unit) {
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
                    Text(record.studentName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Class: ${record.className}", fontSize = 12.sp)
                    Text("Amount: ৳${String.format("%.2f", record.amount)}", fontSize = 12.sp)
                    Text("Method: ${record.paymentMethod}", fontSize = 11.sp)
                    Text("Date: ${dateFormat.format(record.paymentDate)}", fontSize = 11.sp)
                    if (record.isPaid) {
                        Text("✓ Paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    record.remarks?.let {
                        Text("Note: $it", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                IconButton(onClick = { onDelete(record) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(
    students: List<com.teacher.productivitylauncher.data.local.entity.Student>,
    onDismiss: () -> Unit,
    onAddPayment: (Int, String, String, String, String, String) -> Unit
) {
    var selectedStudentIndex by remember { mutableStateOf(0) }
    var amount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var remarks by remember { mutableStateOf("") }
    var studentExpanded by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }

    val paymentMethods = listOf("Cash", "Bank", "Mobile Banking", "Check")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Payment") },
        text = {
            Column {
                // Student Selection
                ExposedDropdownMenuBox(
                    expanded = studentExpanded,
                    onExpandedChange = { studentExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (students.isNotEmpty()) students[selectedStudentIndex].name else "No students",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Student") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = students.isNotEmpty()
                    )
                    if (students.isNotEmpty()) {
                        DropdownMenu(
                            expanded = studentExpanded,
                            onDismissRequest = { studentExpanded = false }
                        ) {
                            students.forEachIndexed { index, student ->
                                DropdownMenuItem(
                                    text = { Text("${student.name} (${student.className})") },
                                    onClick = {
                                        selectedStudentIndex = index
                                        studentExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    leadingIcon = { Text("৳") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Payment Method
                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = it }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    DropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false }
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    paymentMethod = method
                                    methodExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Remarks
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (students.isNotEmpty() && amount.isNotBlank()) {
                        val student = students[selectedStudentIndex]
                        onAddPayment(
                            student.id,
                            student.name,
                            student.className,
                            amount,
                            paymentMethod,
                            remarks
                        )
                    }
                },
                enabled = students.isNotEmpty() && amount.isNotBlank()
            ) {
                Text("Add Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}