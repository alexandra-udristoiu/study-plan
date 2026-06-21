package com.example.studyplan.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface FlashCardDao {

    @Insert
    suspend fun insertFlashCard(card: FlashCardEntity): Long   // returns the generated id
    @Update
    suspend fun updateFlashCard(card: FlashCardEntity)
    @Delete
    suspend fun deleteFlashCard(card: FlashCardEntity)

    // Re-emits automatically whenever this note's cards change.
    @Query("SELECT * FROM flashcards WHERE noteId = :noteId ORDER BY id")
    fun observeFlashCardsForNote(noteId: Int): Flow<List<FlashCardEntity>>

    // A card is due when due <= today (overdue cards from past days are included).
    // The IS NULL branch only covers legacy rows written before due was always set.
    @Query("SELECT * FROM flashcards WHERE due IS NULL OR due <= :today ORDER BY id")
    suspend fun getDueFlashCards(today: LocalDate): List<FlashCardEntity>
}
