package com.teacher.productivitylauncher.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.teacher.productivitylauncher.data.local.dao.*
import com.teacher.productivitylauncher.data.local.entity.*
import com.teacher.productivitylauncher.data.local.util.DateConverter

@Database(
    entities = [
        Student::class,
        ClassRoutine::class,
        Attendance::class,
        Note::class,
        Exam::class,
        FeesRecord::class,
        FeesSummary::class,
        FavoriteApp::class,
        HiddenApp::class,
        BlockedApp::class,
        AppFolder::class,
        RenamedApp::class
    ],
    version = 4,  // Version increased for renamed apps feature
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class TeacherDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun classRoutineDao(): ClassRoutineDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun noteDao(): NoteDao
    abstract fun examDao(): ExamDao
    abstract fun feesDao(): FeesDao
    abstract fun favoriteAppDao(): FavoriteAppDao
    abstract fun hiddenAppDao(): HiddenAppDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun appFolderDao(): AppFolderDao
    abstract fun renamedAppDao(): RenamedAppDao

    companion object {
        @Volatile
        private var INSTANCE: TeacherDatabase? = null

        fun getDatabase(context: Context): TeacherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TeacherDatabase::class.java,
                    "teacher_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}