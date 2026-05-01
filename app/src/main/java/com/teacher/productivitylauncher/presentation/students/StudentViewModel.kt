package com.teacher.productivitylauncher.presentation.students

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.Student
import com.teacher.productivitylauncher.data.local.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class StudentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StudentRepository(
        TeacherDatabase.getDatabase(application).studentDao()
    )

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _studentsWithFees = MutableStateFlow<List<Student>>(emptyList())
    val studentsWithFees: StateFlow<List<Student>> = _studentsWithFees.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    init {
        loadStudents()
        loadStudentsWithFees()
    }

    private fun loadStudents() {
        viewModelScope.launch {
            repository.getAllStudents().collect { studentList ->
                _students.value = studentList
            }
        }
    }

    private fun loadStudentsWithFees() {
        viewModelScope.launch {
            repository.getStudentsWithFeesDue().collect { studentList ->
                _studentsWithFees.value = studentList
            }
        }
    }

    fun addStudent(name: String, className: String, batch: String, phone: String,
                   guardianName: String, guardianPhone: String, feesDue: Double) {
        viewModelScope.launch {
            val student = Student(
                name = name,
                className = className,
                batch = batch,
                phone = phone,
                guardianName = guardianName,
                guardianPhone = guardianPhone,
                feesDue = feesDue,
                joinDate = Date()
            )
            repository.insertStudent(student)
            _message.value = "Student added successfully"
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch {
            repository.updateStudent(student)
            _message.value = "Student updated successfully"
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            _message.value = "Student deleted successfully"
        }
    }

    fun clearMessage() {
        _message.value = ""
    }
}