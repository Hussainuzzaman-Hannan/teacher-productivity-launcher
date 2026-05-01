package com.teacher.productivitylauncher.presentation.feestracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.FeesRecord
import com.teacher.productivitylauncher.data.local.entity.FeesSummary
import com.teacher.productivitylauncher.data.local.repository.FeesRepository
import com.teacher.productivitylauncher.data.local.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FeesViewModel(application: Application) : AndroidViewModel(application) {
    private val feesRepository = FeesRepository(
        TeacherDatabase.getDatabase(application).feesDao()
    )
    private val studentRepository = StudentRepository(
        TeacherDatabase.getDatabase(application).studentDao()
    )

    private val _feesRecords = MutableStateFlow<List<FeesRecord>>(emptyList())
    val feesRecords: StateFlow<List<FeesRecord>> = _feesRecords.asStateFlow()

    private val _unpaidFees = MutableStateFlow<List<FeesRecord>>(emptyList())
    val unpaidFees: StateFlow<List<FeesRecord>> = _unpaidFees.asStateFlow()

    private val _summaries = MutableStateFlow<List<FeesSummary>>(emptyList())
    val summaries: StateFlow<List<FeesSummary>> = _summaries.asStateFlow()

    private val _selectedMonth = MutableStateFlow(getCurrentMonth())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _totalPaid = MutableStateFlow(0.0)
    val totalPaid: StateFlow<Double> = _totalPaid.asStateFlow()

    private val _totalDue = MutableStateFlow(0.0)
    val totalDue: StateFlow<Double> = _totalDue.asStateFlow()

    private val _unpaidCount = MutableStateFlow(0)
    val unpaidCount: StateFlow<Int> = _unpaidCount.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    init {
        loadAllFees()
        loadUnpaidFees()
        loadSummaries()
        loadMonthlyStats()
    }

    private fun getCurrentMonth(): String {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun loadAllFees() {
        viewModelScope.launch {
            feesRepository.getAllFeesRecords().collect { records ->
                _feesRecords.value = records
            }
        }
    }

    private fun loadUnpaidFees() {
        viewModelScope.launch {
            feesRepository.getUnpaidFees().collect { records ->
                _unpaidFees.value = records
            }
        }
    }

    private fun loadSummaries() {
        viewModelScope.launch {
            feesRepository.getAllSummaries().collect { summaries ->
                _summaries.value = summaries
            }
        }
    }

    private fun loadMonthlyStats() {
        viewModelScope.launch {
            val paid = feesRepository.getTotalPaidByMonth(_selectedMonth.value) ?: 0.0
            val due = feesRepository.getTotalDueByMonth(_selectedMonth.value) ?: 0.0
            val count = feesRepository.getUnpaidCountByMonth(_selectedMonth.value)

            _totalPaid.value = paid
            _totalDue.value = due
            _unpaidCount.value = count
        }
    }

    fun changeMonth(month: String) {
        _selectedMonth.value = month
        loadMonthlyStats()
        viewModelScope.launch {
            feesRepository.getFeesByMonth(month).collect { records ->
                _feesRecords.value = records
            }
        }
    }

    fun addFeesPayment(studentId: Int, studentName: String, className: String,
                       amount: Double, paymentMethod: String, remarks: String?) {
        viewModelScope.launch {
            val currentMonth = _selectedMonth.value
            val existingRecord = _feesRecords.value.find {
                it.studentId == studentId && it.month == currentMonth
            }

            if (existingRecord != null) {
                // Update existing record
                val newPaidAmount = existingRecord.paidAmount + amount
                val newDueAmount = existingRecord.dueAmount - amount
                val updatedRecord = existingRecord.copy(
                    paidAmount = newPaidAmount,
                    dueAmount = newDueAmount,
                    paymentDate = Date(),
                    paymentMethod = paymentMethod,
                    remarks = remarks,
                    isPaid = newDueAmount <= 0
                )
                feesRepository.updateFeesRecord(updatedRecord)
                _message.value = "Payment updated successfully"
            } else {
                // Create new record
                val record = FeesRecord(
                    studentId = studentId,
                    studentName = studentName,
                    className = className,
                    amount = amount,
                    paidAmount = amount,
                    dueAmount = 0.0,
                    paymentDate = Date(),
                    paymentMethod = paymentMethod,
                    remarks = remarks,
                    month = currentMonth,
                    isPaid = true
                )
                feesRepository.insertFeesRecord(record)
                _message.value = "Payment recorded successfully"
            }

            updateSummary()
            loadMonthlyStats()
            clearMessageAfterDelay()
        }
    }

    private suspend fun updateSummary() {
        val month = _selectedMonth.value
        val totalPaid = feesRepository.getTotalPaidByMonth(month) ?: 0.0
        val totalDue = feesRepository.getTotalDueByMonth(month) ?: 0.0

        val existingSummary = feesRepository.getSummaryByMonth(month)
        if (existingSummary != null) {
            val updatedSummary = existingSummary.copy(
                totalFees = totalPaid + totalDue,
                totalPaid = totalPaid,
                totalDue = totalDue
            )
            feesRepository.updateSummary(updatedSummary)
        } else {
            val newSummary = FeesSummary(
                className = "All Classes",
                totalFees = totalPaid + totalDue,
                totalPaid = totalPaid,
                totalDue = totalDue,
                month = month
            )
            feesRepository.insertSummary(newSummary)
        }
    }

    fun deleteFeesRecord(record: FeesRecord) {
        viewModelScope.launch {
            feesRepository.deleteFeesRecord(record)
            _message.value = "Record deleted"
            updateSummary()
            loadMonthlyStats()
            clearMessageAfterDelay()
        }
    }

    private fun clearMessageAfterDelay() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _message.value = ""
        }
    }

    fun clearMessage() {
        _message.value = ""
    }

    fun getAvailableMonths(): List<String> {
        val months = mutableSetOf<String>()
        _feesRecords.value.forEach { months.add(it.month) }
        return months.sorted().reversed()
    }
}