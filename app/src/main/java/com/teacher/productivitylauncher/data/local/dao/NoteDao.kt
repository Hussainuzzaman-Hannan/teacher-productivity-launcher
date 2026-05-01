package com.teacher.productivitylauncher.data.local.dao

import androidx.room.*
import com.teacher.productivitylauncher.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, createdDate DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isPinned = 1")
    fun getPinnedNotes(): Flow<List<Note>>

    @Insert
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    // Get all notes (for backup)
    @Query("SELECT * FROM notes")
    fun getAllNotesForBackup(): Flow<List<Note>>

    // Delete all notes (for restore)
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}