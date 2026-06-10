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
 * Retrofit description of the StudyPlanner backend's AI endpoints.
 */
interface SummaryApi {

    /** POST /api/summarize — returns an AI-generated summary for [request].text. */
    @POST("api/summarize")
    suspend fun summarize(@Body request: SummaryRequestDto): SummaryResponseDto

}
