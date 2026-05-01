package com.teacher.productivitylauncher.data.local.repository

import com.teacher.productivitylauncher.data.local.dao.NoteDao
import com.teacher.productivitylauncher.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun getPinnedNotes(): Flow<List<Note>> = noteDao.getPinnedNotes()

    suspend fun insertNote(note: Note) = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
}