package com.example.studyplan.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Request/response payloads for the backend summarisation endpoint.
 *
 * These mirror the Spring DTOs on the server:
 *  - SummaryRequest(text)
 *  - SummaryResponse(summary)
 */
data class SummaryRequestDto(val text: String)

data class SummaryResponseDto(val summary: String)

/**
 * Request/response payloads for the backend flashcard-generation endpoint.
 *
 * These mirror the Spring DTOs on the server:
 *  - FlashCardRequest(title, summary, text)
 *  - the endpoint returns a bare List<FlashCardResponse(front, back)>
 */
data class GenerateFlashCardsRequestDto(
    val title: String,
    val summary: String,
    val text: String,
)

data class FlashCardDto(val front: String, val back: String)

/**
 * Retrofit description of the StudyPlanner backend's AI endpoints.
 */
interface AIApi {

    /** POST /api/summarize — returns an AI-generated summary for [request].text. */
    @POST("api/summarize")
    suspend fun summarize(@Body request: SummaryRequestDto): SummaryResponseDto

    /** POST /api/generate-flashcard — returns AI-generated cards for the note. */
    @POST("api/generate-flashcard")
    suspend fun generateFlashCards(@Body request: GenerateFlashCardsRequestDto): List<FlashCardDto>

}
