package com.example.studyplan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplan.data.FlashCard
import com.example.studyplan.data.FlashCardRepository
import com.example.studyplan.data.StudyNote
import com.example.studyplan.domain.flashcard.FlashCardsGenerationException
import com.example.studyplan.domain.flashcard.FlashCardsGenerator
import com.example.studyplan.domain.flashcard.Rating
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Immutable snapshot shared by the flashcard screens.
 *
 * - [dueCards] are the cards due for review. Loaded once; each action patches it.
 * - [cardsForNote] are the cards for the note open in NoteDetailScreen, reloaded per note.
 * - [isGenerating] is true while an AI generation request is in flight.
 * - [generationError] is non-null when the last generation attempt failed.
 * - [newCardIds] are the ids from the most recent generation, so the cards screen
 *   can badge them as "new".
 */
data class FlashCardsUiState(
    val dueCards: List<FlashCard> = emptyList(),
    val cardsForNote: List<FlashCard> = emptyList(),
    val isGenerating: Boolean = false,
    val generationError: String? = null,
    val newCardIds: Set<Int> = emptySet(),
)

class FlashCardsViewModel(
    private val repository: FlashCardRepository,
    private val flashCardsGenerator: FlashCardsGenerator,
) : ViewModel() {

    // The review-session queue. Deliberately in-memory session state, not a DB
    // projection: reviewCard drops a card from the queue (and re-queues it later
    // this session) without persisting a new due date, so a reactive query can't
    // express it. Loaded once; patched by the actions below.
    private val dueCards = MutableStateFlow<List<FlashCard>>(emptyList())

    // Transient generation/UI state, patched directly.
    private val isGenerating = MutableStateFlow(false)
    private val generationError = MutableStateFlow<String?>(null)
    private val newCardIds = MutableStateFlow<Set<Int>>(emptySet())

    // The note whose cards the management screen is showing (null = none open).
    private val currentNoteId = MutableStateFlow<Int?>(null)

    // Cards for the open note, straight off Room — re-emits on every change, so
    // add/update/delete no longer patch this list by hand. onStart clears the
    // previous note's cards immediately so they don't show for a frame while loading.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val cardsForNote: Flow<List<FlashCard>> = currentNoteId.flatMapLatest { noteId ->
        if (noteId == null) flowOf(emptyList())
        else repository.observeCardsForNote(noteId).onStart { emit(emptyList()) }
    }

    val uiState: StateFlow<FlashCardsUiState> = combine(
        dueCards, cardsForNote, isGenerating, generationError, newCardIds,
    ) { due, cards, generating, error, newIds ->
        FlashCardsUiState(
            dueCards = due,
            cardsForNote = cards,
            isGenerating = generating,
            generationError = error,
            newCardIds = newIds,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FlashCardsUiState())

    init {
        viewModelScope.launch {
            dueCards.value = repository.getDueCards()
        }
    }

    /** Points the reactive cards flow at [noteId] when the cards screen opens. */
    fun loadCardsForNote(noteId: Int) {
        currentNoteId.value = noteId
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
            isGenerating.value = true
            generationError.value = null
            newCardIds.value = emptySet()
            try {
                val generated = flashCardsGenerator.generateFlashCards(note)
                // addCard assigns a fresh schedule and the real id, so we
                // persist front/back rather than the unsaved generated card.
                val saved = generated.map { persistNewCard(note.id, it.front, it.back) }
                newCardIds.value = saved.map { it.id }.toSet()
                onGenerated()
            } catch (e: FlashCardsGenerationException) {
                generationError.value = e.message ?: "Failed to generate flashcards."
            } finally {
                isGenerating.value = false
            }
        }
    }

    /** Clears the "new" badges once the user leaves the cards screen. */
    fun clearNewCardIds() {
        newCardIds.value = emptySet()
    }

    /**
     * Inserts a new card and returns it. The cards-for-note list updates itself via
     * the Flow; we still patch the in-memory due queue, since new cards are due now.
     */
    private suspend fun persistNewCard(noteId: Int, front: String, back: String): FlashCard {
        val card = repository.addCard(noteId, front, back)
        dueCards.value = dueCards.value + card
        return card
    }

    /** Clears any generation error after the UI has shown it. */
    fun clearGenerationError() {
        generationError.value = null
    }

    fun updateCard(card: FlashCard) {
        viewModelScope.launch {
            repository.updateCard(card)
            dueCards.value = dueCards.value.map { if (it.id == card.id) card else it }
        }
    }

    fun deleteCard(card: FlashCard) {
        viewModelScope.launch {
            repository.deleteCard(card)
            dueCards.value = dueCards.value.filterNot { it.id == card.id }
        }
    }

    fun reviewCard(card: FlashCard, rating: Rating) {
        viewModelScope.launch {
            val due = card.schedule.review(rating)
            repository.updateCard(card)
            val stillDueToday = !due.isAfter(LocalDate.now())
            val remaining = dueCards.value.filterNot { it.id == card.id }
            // Re-show later this session instead of immediately when still due today.
            dueCards.value = if (stillDueToday) remaining + card else remaining
        }
    }
}
