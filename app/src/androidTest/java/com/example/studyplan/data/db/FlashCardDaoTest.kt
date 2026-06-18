package com.example.studyplan.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Verifies the "cards due for review" query against a real in-memory SQLite DB,
 * since the overdue logic lives in SQL (`due <= today`) rather than in Kotlin.
 */
@RunWith(AndroidJUnit4::class)
class FlashCardDaoTest {

    private lateinit var db: StudyPlanDatabase
    private lateinit var dao: FlashCardDao
    private var noteId = 0

    private val today: LocalDate = LocalDate.of(2026, 6, 17)

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StudyPlanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.flashCardDao()
        // Flashcards need a parent note (foreign key).
        db.noteDao().insertNote(NoteEntity(title = "t", topicName = "topic", content = "c", summary = "s"))
        noteId = db.noteDao().getAllNotes().first().id
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun getDueFlashCards_includesOverdueDueTodayAndNewCards_butNotFutureCards() = runBlocking {
        val overdue = insertCard(front = "overdue", due = today.minusDays(1))
        val dueToday = insertCard(front = "today", due = today)
        val newCard = insertCard(front = "new", due = null)
        insertCard(front = "future", due = today.plusDays(1))   // should be excluded

        val dueFronts = dao.getDueFlashCards(today).map { it.front }

        assertEquals(listOf(overdue, dueToday, newCard), dueFronts)
    }

    private suspend fun insertCard(front: String, due: LocalDate?): String {
        dao.insertFlashCard(
            FlashCardEntity(noteId = noteId, front = front, back = "back", due = due, statePayload = "{}")
        )
        return front
    }
}
