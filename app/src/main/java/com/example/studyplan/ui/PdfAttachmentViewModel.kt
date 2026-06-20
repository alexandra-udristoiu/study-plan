package com.example.studyplan.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplan.data.pdf.PdfExtractionException
import com.example.studyplan.data.pdf.PdfTextExtractor
import kotlinx.coroutines.launch

/**
 * Owns reading a picked PDF into text. Mirrors [SummaryViewModel]: it only does
 * the extraction — deciding what to do with the resulting text (e.g. dropping it
 * into a note's content) is wired up by the screen/caller.
 */
class PdfAttachmentViewModel(
    private val pdfTextExtractor: PdfTextExtractor,
) : ViewModel() {

    var content by mutableStateOf<String?>(null)
        private set

    var isGenerating by mutableStateOf(false)
        private set

    // Non-null when the last generation attempt failed; holds a message to show.
    var error by mutableStateOf<String?>(null)
        private set

    /**
     * Reads the PDF at [uri] and holds the extracted text.
     *
     * Suggested shape: launch in viewModelScope, flip an "extracting" flag,
     * call pdfTextExtractor.extractText(uri) inside try/catch(PdfExtractionException),
     * store the result/error.
     */
    fun extract(uri: Uri) {
        viewModelScope.launch {
            isGenerating = true
            content = null
            error = null
            try {
                content = pdfTextExtractor.extractText(uri)
            } catch (e: PdfExtractionException) {
                error = e.message ?: "Failed to read the PDF."
            } finally {
                isGenerating = false
            }
        }
    }

    /** Clears the last result so it isn't re-applied after the screen consumes it. */
    fun clear() {
        content = null
        error = null
    }

    /** Clears only the error, e.g. once the screen has shown it in a snackbar. */
    fun clearError() {
        error = null
    }
}
