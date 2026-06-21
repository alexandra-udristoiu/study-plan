package com.example.studyplan.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM study_notes ORDER BY id")
    suspend fun getAllNotes() : List<NoteEntity>
    // Re-emits automatically whenever the study_notes table changes.
    @Query("SELECT * FROM study_notes ORDER BY id")
    fun observeAllNotes(): Flow<List<NoteEntity>>
    @Insert
    suspend fun insertNote(note: NoteEntity)
    @Update
    suspend fun updateNote(note: NoteEntity)
    @Delete
    suspend fun deleteNote(note: NoteEntity)
    @Query("DELETE FROM study_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)
    @Query("SELECT * FROM study_notes WHERE topicName = :topic ORDER BY id")
    suspend fun getNotesByTopic(topic: String): List<NoteEntity>
    @Query("SELECT * FROM study_notes WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteEntity?
}
