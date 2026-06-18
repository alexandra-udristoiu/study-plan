package com.example.studyplan

import android.content.Context
import com.example.studyplan.data.FlashCardRepository
import com.example.studyplan.data.NoteRepository
import com.example.studyplan.data.db.StudyPlanDatabase
import com.example.studyplan.data.remote.BackendApi
import com.example.studyplan.domain.flashcard.AIFlashCardsGenerator
import com.example.studyplan.domain.flashcard.FlashCardsGenerator
import com.example.studyplan.domain.flashcard.Sm2ScheduleFactory
import com.example.studyplan.domain.summary.AISummaryGenerator
import com.example.studyplan.domain.summary.SummaryGenerator

/**
 * Process-wide dependency container (manual DI). Built once in [StudyPlanApplication]
 * and shared by every screen, so wiring lives in one place instead of each Activity.
 *
 * Everything is `lazy`, so a dependency is only created the first time it's used.
 */
class AppContainer(context: Context) {

    private val database = StudyPlanDatabase.getInstance(context)

    val noteRepository: NoteRepository by lazy {
        NoteRepository(database.noteDao())
    }

    val flashCardRepository: FlashCardRepository by lazy {
        FlashCardRepository(database.flashCardDao(), Sm2ScheduleFactory())
    }

    val summaryGenerator: SummaryGenerator by lazy {
        AISummaryGenerator(BackendApi.aiApi)
    }

    val flashCardsGenerator: FlashCardsGenerator by lazy {
        AIFlashCardsGenerator(BackendApi.aiApi, Sm2ScheduleFactory())
    }
}
