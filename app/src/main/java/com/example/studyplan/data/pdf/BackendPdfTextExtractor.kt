package com.example.studyplan.data.pdf

import android.content.ContentResolver
import android.net.Uri
import com.example.studyplan.data.remote.PdfApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException

/**
 * A [PdfTextExtractor] that uploads the picked PDF to the StudyPlanner backend
 * (POST /api/extract-pdf) and returns the text the server extracts.
 *
 * @param contentResolver used to read the bytes behind the picked [Uri].
 * @param pdfApi the Retrofit service; inject [BackendApi.pdfApi] in production.
 */
class BackendPdfTextExtractor(
    private val contentResolver: ContentResolver,
    private val pdfApi: PdfApi,
) : PdfTextExtractor {

    override suspend fun extractText(uri: Uri): String {
        // Reading the Uri is blocking IO, so do it off the main thread.
        val bytes = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: throw PdfExtractionException("Couldn't open the selected PDF.")

        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = "upload.pdf",
            body = bytes.toRequestBody("application/pdf".toMediaType()),
        )

        // Retrofit runs the suspend call off the main thread, so no withContext needed.
        return try {
            pdfApi.extractPdf(part).text.trim()
        } catch (e: IOException) {
            // No connection, DNS failure, or timeout — the request never
            // completed. Most likely the backend isn't running/reachable.
            throw PdfExtractionException(
                "Couldn't reach the PDF service. Check that the backend is running and try again.",
                e,
            )
        } catch (e: HttpException) {
            // The backend responded, but with a 4xx/5xx status.
            throw PdfExtractionException(
                "The PDF service returned an error (${e.code()}). Please try again.",
                e,
            )
        }
    }
}
