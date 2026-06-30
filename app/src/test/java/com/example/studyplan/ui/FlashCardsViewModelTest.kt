package com.example.studyplan.ui

import com.example.studyplan.data.FlashCard
import com.example.studyplan.data.FlashCardRepository
import com.example.studyplan.data.StudyNote
import com.example.studyplan.data.db.FlashCardDao
import com.example.studyplan.data.db.FlashCardEntity
import com.example.studyplan.domain.flashcard.FlashCardsGenerator
import com.example.studyplan.domain.flashcard.Rating
import com.example.studyplan.domain.flashcard.Sm2ScheduleFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Covers the in-memory due-queue logic in [FlashCardsViewModel] — the part the DAO
 * and SM-2 tests don't reach. The rule swaps Dispatchers.Main for a test dispatcher
 * so viewModelScope coroutines run on the test's virtual clock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FlashCardsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun reviewCard_withGood_removesCardFromTheDueQueue() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModelWithOneDueCard()
            val state = viewModel.uiState.value
            val card = state.dueCards.single()

            // A first "Good" pushes the next review to tomorrow, so it leaves today's queue.
            viewModel.reviewCard(card, Rating.GOOD)
            advanceUntilIdle()

            val dueCards = viewModel.uiState.value.dueCards
            assertEquals(emptyList<FlashCard>(), dueCards)
        }

    @Test
    fun reviewCard_withAgain_keepsCardInTheDueQueue() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModelWithOneDueCard()
            val state = viewModel.uiState.value
            val card = state.dueCards.single()

            // "Again" leaves the card due today, so it's re-queued for later this session.
            viewModel.reviewCard(card, Rating.AGAIN)
            advanceUntilIdle()

            val dueCards = viewModel.uiState.value.dueCards
            assertEquals(listOf(card.id), dueCards.map { it.id })
        }

    /**
     * Builds a ViewModel whose repository already holds one card due today, then waits
     * for `init` to load it. A live collector keeps the `WhileSubscribed` uiState active
     * so reading `.value` reflects the latest combined state.
     */
    private suspend fun TestScope.viewModelWithOneDueCard(): FlashCardsViewModel {
        val repository = FlashCardRepository(FakeFlashCardDao(), Sm2ScheduleFactory())
        repository.addCard(noteId = 1, front = "Capital of France?", back = "Paris")

        val viewModel = FlashCardsViewModel(repository, EmptyFlashCardsGenerator)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        return viewModel
    }
}

/** In-memory stand-in for the Room DAO — just enough for the due-queue tests. */
private class FakeFlashCardDao : FlashCardDao {
    private val cards = mutableListOf<FlashCardEntity>()
    private var nextId = 1

    override suspend fun insertFlashCard(card: FlashCardEntity): Long {
        val saved = card.copy(id = nextId++)
        cards += saved
        return saved.id.toLong()
    }

    override suspend fun updateFlashCard(card: FlashCardEntity) {
        val index = cards.indexOfFirst { it.id == card.id }
        if (index != -1) cards[index] = card
    }

    override suspend fun deleteFlashCard(card: FlashCardEntity) {
        cards.removeAll { it.id == card.id }
    }

    override fun observeFlashCardsForNote(noteId: Int): Flow<List<FlashCardEntity>> =
        MutableStateFlow(cards.filter { it.noteId == noteId })

    // Mirrors the real query: a card is due unless its date is in the future.
    override suspend fun getDueFlashCards(today: LocalDate): List<FlashCardEntity> =
        cards.filter { it.due?.isAfter(today) != true }
}

/** The tests never generate, so this just satisfies the constructor. */
private object EmptyFlashCardsGenerator : FlashCardsGenerator {
    override suspend fun generateFlashCards(note: StudyNote): List<FlashCard> = emptyList()
}
