package com.example.studyplan.data

import com.example.studyplan.data.db.NoteDao
import com.example.studyplan.data.db.toStudyNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepository(private val dao: NoteDao) {

    /** Emits the full notes list and re-emits on every change to the table. */
    fun observeNotes(): Flow<List<StudyNote>> =
        dao.observeAllNotes().map { entities -> entities.map { it.toStudyNote() } }

    suspend fun addNote(title: String, topicName: String, content: String, summary: String) {
        dao.insertNote(StudyNote(0, title, topicName, content, summary).toNoteEntity())
    }

    suspend fun deleteNote(noteId: Int) {
        dao.deleteNoteById(noteId)
    }

    suspend fun updateNote(note: StudyNote) {
        dao.updateNote(note.toNoteEntity())
    }

    suspend fun filterByTopic(topicName: String): List<StudyNote> =
        dao.getNotesByTopic(topicName).map { it.toStudyNote() }

    suspend fun getNoteById(id: Int): StudyNote? =
        dao.getNoteById(id)?.toStudyNote()
}
