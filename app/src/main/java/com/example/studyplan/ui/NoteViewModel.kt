package com.example.studyplan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplan.data.NoteRepository
import com.example.studyplan.data.StudyNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the notes list screen needs to render, in one immutable snapshot.
 *
 * - [notes] is the list actually shown: all notes, or just the [selectedTopic].
 * - [topics] are the distinct, non-empty topics used to build the filter chips.
 * - [selectedTopic] is the active filter (null = "All").
 */
data class NotesUiState(
    val notes: List<StudyNote> = emptyList(),
    val topics: List<String> = emptyList(),
    val selectedTopic: String? = null,
)

class NoteViewModel(
    private val repository: NoteRepository,
) : ViewModel() {

    // The full, unfiltered list, kept hot off the Room Flow — it re-emits on its own
    // whenever the table changes, so mutations below are fire-and-forget (no reloads).
    private val allNotes: StateFlow<List<StudyNote>> = repository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // null = "All". When set, only notes with this topic are shown.
    private val selectedTopic = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NotesUiState> =
        combine(allNotes, selectedTopic) { notes, topic ->
            val visibleNotes = if (topic == null) notes else notes.filter { it.topicName == topic }
            val topics = notes
                .map { it.topicName }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
            NotesUiState(notes = visibleNotes, topics = topics, selectedTopic = topic)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    fun selectTopic(topic: String?) {
        selectedTopic.value = topic
    }

    fun addNote(title: String, topicName: String, content: String, summary: String) {
        viewModelScope.launch {
            repository.addNote(title, topicName, content, summary)
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    fun updateNote(note: StudyNote) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun findById(noteId: Int): StudyNote? = allNotes.value.find { it.id == noteId }
}
