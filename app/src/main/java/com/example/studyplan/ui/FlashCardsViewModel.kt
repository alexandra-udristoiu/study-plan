package com.example.studyplan.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplan.data.FlashCard
import com.example.studyplan.data.FlashCardRepository
import com.example.studyplan.data.StudyNote
import com.example.studyplan.domain.flashcard.FlashCardsGenerationException
import com.example.studyplan.domain.flashcard.FlashCardsGenerator
import com.example.studyplan.domain.flashcard.Rating
import kotlinx.coroutines.launch
import java.time.LocalDate

class FlashCardsViewModel(
    private val repository: FlashCardRepository,
    private val flashCardsGenerator: FlashCardsGenerator,
) : ViewModel() {

    // The cards due for review. Loaded once; each action below patches it
    // instead of re-querying.
    var dueCards by mutableStateOf<List<FlashCard>>(emptyList())
        private set

    // The cards belonging to the note currently open in NoteDetailScreen.
    // Reloaded per note; add/update/delete patch it in memory.
    var cardsForNote by mutableStateOf<List<FlashCard>>(emptyList())
        private set

    // True while an AI generation request is in flight (for showing a spinner).
    var isGenerating by mutableStateOf(false)
        private set

    // Non-null when the last generation attempt failed; holds a message to show.
    var generationError by mutableStateOf<String?>(null)
        private set

    // Ids of the cards from the most recent generation, so the cards screen can
    // badge them as "new". In-memory only; keyed by id so it still matches after
    // the cards screen re-queries the DB on open. Cleared when leaving the screen.
    var newCardIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    init {
        viewModelScope.launch {
            dueCards = repository.getDueCards()
        }
    }

    /** Loads the cards for [noteId] into [cardsForNote] when the cards screen opens. */
    fun loadCardsForNote(noteId: Int) {
        // Drop the previous note's cards so they don't show for a frame while loading.
        cardsForNote = emptyList()
        viewModelScope.launch {
            cardsForNote = repository.getCardsForNote(noteId)
        }
    }

    fun addCard(noteId: Int, front: String, back: String) {
        viewModelScope.launch {
            persistNewCard(noteId, front, back)
        }
    }

    /**
     * Asks the AI generator for cards from [note], persists each one, and patches
     * the in-memory lists — mirroring [addCard], just in bulk. [onGenerated] runs
     * only on success, so the caller can navigate to the cards once they exist.
     */
    fun generateCards(note: StudyNote, onGenerated: () -> Unit = {}) {
        viewModelScope.launch {
            isGenerating = true
            generationError = null
            newCardIds = emptySet()
            try {
                val generated = flashCardsGenerator.generateFlashCards(note)
                // addCard assigns a fresh schedule and the real id, so we
                // persist front/back rather than the unsaved generated card.
                val saved = generated.map { persistNewCard(note.id, it.front, it.back) }
                newCardIds = saved.map { it.id }.toSet()
                onGenerated()
            } catch (e: FlashCardsGenerationException) {
                generationError = e.message ?: "Failed to generate flashcards."
            } finally {
                isGenerating = false
            }
        }
    }

    /** Clears the "new" badges once the user leaves the cards screen. */
    fun clearNewCardIds() {
        newCardIds = emptySet()
    }

    /** Inserts a new card, appends it to both in-memory lists, and returns it. */
    private suspend fun persistNewCard(noteId: Int, front: String, back: String): FlashCard {
        // A new card has no review date yet, so it's due immediately.
        val card = repository.addCard(noteId, front, back)
        dueCards = dueCards + card
        cardsForNote = cardsForNote + card
        return card
    }

    /** Clears any generation error after the UI has shown it. */
    fun clearGenerationError() {
        generationError = null
    }

    fun updateCard(card: FlashCard) {
        viewModelScope.launch {
            repository.updateCard(card)
            dueCards = dueCards.map { if (it.id == card.id) card else it }
            cardsForNote = cardsForNote.map { if (it.id == card.id) card else it }
        }
    }

    fun deleteCard(card: FlashCard) {
        viewModelScope.launch {
            repository.deleteCard(card)
            dueCards = dueCards.filterNot { it.id == card.id }
            cardsForNote = cardsForNote.filterNot { it.id == card.id }
        }
    }

    fun reviewCard(card: FlashCard, rating: Rating) {
        viewModelScope.launch {
            val due = card.schedule.review(rating)
            repository.updateCard(card)
            val stillDueToday = !due.isAfter(LocalDate.now())
            dueCards = dueCards.filterNot { it.id == card.id }
            if (stillDueToday) {
                // Re-show later this session instead of immediately.
                dueCards = dueCards + card
            }
        }
    }
}
