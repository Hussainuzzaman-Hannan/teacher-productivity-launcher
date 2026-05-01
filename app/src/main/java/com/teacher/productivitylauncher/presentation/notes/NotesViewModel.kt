package com.teacher.productivitylauncher.presentation.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.productivitylauncher.data.local.database.TeacherDatabase
import com.teacher.productivitylauncher.data.local.entity.Note
import com.teacher.productivitylauncher.data.local.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NoteRepository(
        TeacherDatabase.getDatabase(application).noteDao()
    )

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _pinnedNotes = MutableStateFlow<List<Note>>(emptyList())
    val pinnedNotes: StateFlow<List<Note>> = _pinnedNotes.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    init {
        loadNotes()
        loadPinnedNotes()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            repository.getAllNotes().collect { noteList ->
                _notes.value = noteList
            }
        }
    }

    private fun loadPinnedNotes() {
        viewModelScope.launch {
            repository.getPinnedNotes().collect { noteList ->
                _pinnedNotes.value = noteList
            }
        }
    }

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            val note = Note(
                title = title.ifEmpty { "Untitled" },
                content = content,
                createdDate = Date(),
                isPinned = false
            )
            repository.insertNote(note)
            _message.value = "Note added successfully"
            clearMessageAfterDelay()
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
            _message.value = "Note updated"
            clearMessageAfterDelay()
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            _message.value = "Note deleted"
            clearMessageAfterDelay()
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            val updatedNote = note.copy(isPinned = !note.isPinned)
            repository.updateNote(updatedNote)
            _message.value = if (updatedNote.isPinned) "Note pinned" else "Note unpinned"
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