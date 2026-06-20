package com.example.studyplan.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Response payload for the backend PDF text-extraction endpoint.
 *
 * Mirrors the Spring DTO on the server:
 *  - ExtractPdfResponse(text)
 */
data class ExtractPdfResponseDto(val text: String)

/**
 * Retrofit description of the StudyPlanner backend's PDF endpoints.
 *
 * Kept separate from [AIApi] because extraction is plain PDFBox text-stripping,
 * not an AI/LLM call — mirroring the server's separate PdfController.
 */
interface PdfApi {

    /**
     * POST /api/extract-pdf — uploads a PDF and returns its extracted text.
     *
     * Sent as multipart/form-data; the [file] part name must match the server's
     * @RequestParam("file") MultipartFile.
     */
    @Multipart
    @POST("api/extract-pdf")
    suspend fun extractPdf(@Part file: MultipartBody.Part): ExtractPdfResponseDto
}
