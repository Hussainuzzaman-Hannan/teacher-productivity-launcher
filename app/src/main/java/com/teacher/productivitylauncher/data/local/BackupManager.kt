package com.teacher.productivitylauncher.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {

    private val database = TeacherDatabase.getDatabase(context)

    // Export database to JSON file
    suspend fun exportDatabase(): File? = withContext(Dispatchers.IO) {
        try {
            val jsonObject = JSONObject().apply {
                put("export_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("version", 1)

                // Export Students
                val studentsArray = JSONArray()
                val students = database.studentDao().getAllStudents()
                students.collect { studentList ->
                    studentList.forEach { student ->
                        studentsArray.put(JSONObject().apply {
                            put("name", student.name)
                            put("className", student.className)
                            put("batch", student.batch)
                            put("phone", student.phone)
                            put("guardianName", student.guardianName)
                            put("guardianPhone", student.guardianPhone)
                            put("feesDue", student.feesDue)
                            put("joinDate", student.joinDate.time)
                            put("isActive", student.isActive)
                        })
                    }
                }
                put("students", studentsArray)

                // Export Class Routines
                val routinesArray = JSONArray()
                val routines = database.classRoutineDao().getRoutineByClass("")
                routines.collect { routineList ->
                    routineList.forEach { routine ->
                        routinesArray.put(JSONObject().apply {
                            put("subjectName", routine.subjectName)
                            put("className", routine.className)
                            put("teacherName", routine.teacherName)
                            put("dayOfWeek", routine.dayOfWeek)
                            put("startTime", routine.startTime)
                            put("endTime", routine.endTime)
                            put("roomNumber", routine.roomNumber)
                            put("notificationEnabled", routine.notificationEnabled)
                        })
                    }
                }
                put("class_routines", routinesArray)

                // Export Attendance
                val attendanceArray = JSONArray()
                val attendanceList = database.attendanceDao().getAttendanceByDate(Date())
                attendanceList.collect { attendanceRecords ->
                    attendanceRecords.forEach { attendance ->
                        attendanceArray.put(JSONObject().apply {
                            put("studentId", attendance.studentId)
                            put("className", attendance.className)
                            put("date", attendance.date.time)
                            put("isPresent", attendance.isPresent)
                            put("remarks", attendance.remarks ?: "")
                        })
                    }
                }
                put("attendance", attendanceArray)

                // Export Notes
                val notesArray = JSONArray()
                val notes = database.noteDao().getAllNotes()
                notes.collect { noteList ->
                    noteList.forEach { note ->
                        notesArray.put(JSONObject().apply {
                            put("title", note.title)
                            put("content", note.content)
                            put("createdDate", note.createdDate.time)
                            put("isPinned", note.isPinned)
                        })
                    }
                }
                put("notes", notesArray)

                // Export Exams
                val examsArray = JSONArray()
                val exams = database.examDao().getAllExams()
                exams.collect { examList ->
                    examList.forEach { exam ->
                        examsArray.put(JSONObject().apply {
                            put("examName", exam.examName)
                            put("className", exam.className)
                            put("examDate", exam.examDate.time)
                            put("description", exam.description ?: "")
                        })
                    }
                }
                put("exams", examsArray)
            }

            val backupFile = File(context.filesDir, "backup_${System.currentTimeMillis()}.json")
            FileOutputStream(backupFile).use { outputStream ->
                outputStream.write(jsonObject.toString().toByteArray())
            }

            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Import database from JSON file
    suspend fun importDatabase(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            // Clear existing data first
            clearAllData()

            // Import Students
            if (jsonObject.has("students")) {
                val studentsArray = jsonObject.getJSONArray("students")
                for (i in 0 until studentsArray.length()) {
                    val studentJson = studentsArray.getJSONObject(i)
                    val student = Student(
                        name = studentJson.getString("name"),
                        className = studentJson.getString("className"),
                        batch = studentJson.getString("batch"),
                        phone = studentJson.getString("phone"),
                        guardianName = studentJson.getString("guardianName"),
                        guardianPhone = studentJson.getString("guardianPhone"),
                        feesDue = studentJson.getDouble("feesDue"),
                        joinDate = Date(studentJson.getLong("joinDate")),
                        isActive = studentJson.optBoolean("isActive", true)
                    )
                    database.studentDao().insertStudent(student)
                }
            }

            // Import Class Routines
            if (jsonObject.has("class_routines")) {
                val routinesArray = jsonObject.getJSONArray("class_routines")
                for (i in 0 until routinesArray.length()) {
                    val routineJson = routinesArray.getJSONObject(i)
                    val routine = ClassRoutine(
                        subjectName = routineJson.getString("subjectName"),
                        className = routineJson.getString("className"),
                        teacherName = routineJson.getString("teacherName"),
                        dayOfWeek = routineJson.getInt("dayOfWeek"),
                        startTime = routineJson.getString("startTime"),
                        endTime = routineJson.getString("endTime"),
                        roomNumber = routineJson.getString("roomNumber"),
                        notificationEnabled = routineJson.optBoolean("notificationEnabled", true)
                    )
                    database.classRoutineDao().insertRoutine(routine)
                }
            }

            // Import Attendance
            if (jsonObject.has("attendance")) {
                val attendanceArray = jsonObject.getJSONArray("attendance")
                for (i in 0 until attendanceArray.length()) {
                    val attendanceJson = attendanceArray.getJSONObject(i)
                    val attendance = Attendance(
                        studentId = attendanceJson.getInt("studentId"),
                        className = attendanceJson.getString("className"),
                        date = Date(attendanceJson.getLong("date")),
                        isPresent = attendanceJson.getBoolean("isPresent"),
                        remarks = attendanceJson.optString("remarks", null)
                    )
                    database.attendanceDao().insertAttendance(attendance)
                }
            }

            // Import Notes
            if (jsonObject.has("notes")) {
                val notesArray = jsonObject.getJSONArray("notes")
                for (i in 0 until notesArray.length()) {
                    val noteJson = notesArray.getJSONObject(i)
                    val note = Note(
                        title = noteJson.getString("title"),
                        content = noteJson.getString("content"),
                        createdDate = Date(noteJson.getLong("createdDate")),
                        isPinned = noteJson.optBoolean("isPinned", false)
                    )
                    database.noteDao().insertNote(note)
                }
            }

            // Import Exams
            if (jsonObject.has("exams")) {
                val examsArray = jsonObject.getJSONArray("exams")
                for (i in 0 until examsArray.length()) {
                    val examJson = examsArray.getJSONObject(i)
                    val exam = Exam(
                        examName = examJson.getString("examName"),
                        className = examJson.getString("className"),
                        examDate = Date(examJson.getLong("examDate")),
                        description = examJson.optString("description", null)
                    )
                    database.examDao().insertExam(exam)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun clearAllData() {
        // Clear all tables
        val students = database.studentDao().getAllStudents()
        students.collect { studentList ->
            studentList.forEach { student ->
                database.studentDao().deleteStudent(student)
            }
        }

        val routines = database.classRoutineDao().getRoutineByClass("")
        routines.collect { routineList ->
            routineList.forEach { routine ->
                database.classRoutineDao().deleteRoutine(routine)
            }
        }

        val attendance = database.attendanceDao().getAttendanceByDate(Date())
        attendance.collect { attendanceList ->
            attendanceList.forEach { attendanceRecord ->
                database.attendanceDao().deleteAttendance(attendanceRecord)
            }
        }

        val notes = database.noteDao().getAllNotes()
        notes.collect { noteList ->
            noteList.forEach { note ->
                database.noteDao().deleteNote(note)
            }
        }

        val exams = database.examDao().getAllExams()
        exams.collect { examList ->
            examList.forEach { exam ->
                database.examDao().deleteExam(exam)
            }
        }
    }

    fun shareBackupFile(file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(
                context,
                authority,
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing backup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun pickBackupFile(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }
}