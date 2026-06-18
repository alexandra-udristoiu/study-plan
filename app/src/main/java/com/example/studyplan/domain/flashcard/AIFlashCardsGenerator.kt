package com.example.studyplan.domain.flashcard

import com.example.studyplan.data.FlashCard
import com.example.studyplan.data.StudyNote
import com.example.studyplan.data.remote.AIApi
import com.example.studyplan.data.remote.GenerateFlashCardsRequestDto
import retrofit2.HttpException
import java.io.IOException

/**
 * A [FlashCardsGenerator] that asks the StudyPlanner backend (POST /api/generate-flashcard)
 * for real, AI-generated flashcards.
 *
 * @param aiApi the Retrofit service; inject [BackendApi.aiApi] in production.
 * @param scheduleFactory supplies a fresh [CardSchedule] for each new card.
 */
class AIFlashCardsGenerator(
    private val aiApi: AIApi,
    private val scheduleFactory: CardScheduleFactory,
) : FlashCardsGenerator {

    override suspend fun generateFlashCards(note: StudyNote): List<FlashCard> {
        val text = note.content.trim()
        if (text.isEmpty()) return emptyList()

        return try {
            val request = GenerateFlashCardsRequestDto(
                title = note.title,
                summary = note.summary,
                text = text,
            )
            aiApi.generateFlashCards(request)
                .map { dto ->
                    FlashCard(
                        // Not yet persisted — the DB assigns the real id on insert.
                        id = 0,
                        noteId = note.id,
                        front = dto.front.trim(),
                        back = dto.back.trim(),
                        schedule = scheduleFactory.newSchedule(),
                    )
                }
        } catch (e: IOException) {
            // No connection, DNS failure, or timeout — the request never
            // completed. Most likely the backend isn't running/reachable.
            throw FlashCardsGenerationException(
                "Couldn't reach the flashcard service. Check that the backend is running and try again.",
                e,
            )
        } catch (e: HttpException) {
            // The backend responded, but with a 4xx/5xx status.
            throw FlashCardsGenerationException(
                "The flashcard service returned an error (${e.code()}). Please try again.",
                e,
            )
        }
    }
}
