package com.example.studyplan.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplan.data.pdf.PdfExtractionException
import com.example.studyplan.data.pdf.PdfTextExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Immutable snapshot of an in-progress PDF text extraction. */
data class PdfAttachmentUiState(
    val content: String? = null,
    val isGenerating: Boolean = false,
    // Non-null when the last extraction attempt failed; holds a message to show.
    val error: String? = null,
)

/**
 * Owns reading a picked PDF into text. Mirrors [SummaryViewModel]: it only does
 * the extraction — deciding what to do with the resulting text (e.g. dropping it
 * into a note's content) is wired up by the screen/caller.
 */
class PdfAttachmentViewModel(
    private val pdfTextExtractor: PdfTextExtractor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfAttachmentUiState())
    val uiState: StateFlow<PdfAttachmentUiState> = _uiState.asStateFlow()

    /** Reads the PDF at [uri] and holds the extracted text. */
    fun extract(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, content = null, error = null) }
            try {
                val text = pdfTextExtractor.extractText(uri)
                _uiState.update { it.copy(content = text) }
            } catch (e: PdfExtractionException) {
                _uiState.update { it.copy(error = e.message ?: "Failed to read the PDF.") }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    /** Clears the last result so it isn't re-applied after the screen consumes it. */
    fun clear() {
        _uiState.update { it.copy(content = null, error = null) }
    }

    /** Clears only the error, e.g. once the screen has shown it in a snackbar. */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
