package com.example.studyplan.data.pdf

import android.net.Uri

/**
 * Extracts the plain text of a PDF the user picked.
 *
 * Takes the [Uri] handed back by the document picker and returns the document's
 * text. Reading the [Uri] is a data-access concern, so it lives here rather than
 * in the ViewModel — callers just hand over the picked PDF and get text back.
 */
interface PdfTextExtractor {

    /**
     * Reads the PDF at [uri] and returns its extracted text.
     *
     * @throws PdfExtractionException if the PDF can't be read or the backend fails.
     */
    suspend fun extractText(uri: Uri): String
}
