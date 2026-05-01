package com.teacher.productivitylauncher.presentation.exams

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.Exam
import com.teacher.productivitylauncher.data.local.repository.ExamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit

class ExamsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExamRepository(
        TeacherDatabase.getDatabase(application).examDao()
    )

    private val _exams = MutableStateFlow<List<Exam>>(emptyList())
    val exams: StateFlow<List<Exam>> = _exams.asStateFlow()

    private val _nextExam = MutableStateFlow<Exam?>(null)
    val nextExam: StateFlow<Exam?> = _nextExam.asStateFlow()

    private val _daysRemaining = MutableStateFlow(0)
    val daysRemaining: StateFlow<Int> = _daysRemaining.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    init {
        loadExams()
    }

    private fun loadExams() {
        viewModelScope.launch {
            repository.getAllExams().collect { examList ->
                _exams.value = examList
                updateNextExam()
            }
        }
    }

    private fun updateNextExam() {
        val currentDate = Date()
        val futureExams = _exams.value.filter { it.examDate >= currentDate }.sortedBy { it.examDate }
        _nextExam.value = futureExams.firstOrNull()

        _nextExam.value?.let { exam ->
            val diff = exam.examDate.time - currentDate.time
            val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
            _daysRemaining.value = days.coerceAtLeast(0)
        } ?: run {
            _daysRemaining.value = 0
        }
    }

    fun addExam(examName: String, className: String, examDate: Date, description: String?) {
        viewModelScope.launch {
            val exam = Exam(
                examName = examName,
                className = className,
                examDate = examDate,
                description = description
            )
            repository.insertExam(exam)
            _message.value = "Exam added successfully"
            clearMessageAfterDelay()
        }
    }

    fun updateExam(exam: Exam) {
        viewModelScope.launch {
            repository.updateExam(exam)
            _message.value = "Exam updated"
            clearMessageAfterDelay()
        }
    }

    fun deleteExam(exam: Exam) {
        viewModelScope.launch {
            repository.deleteExam(exam)
            _message.value = "Exam deleted"
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
}