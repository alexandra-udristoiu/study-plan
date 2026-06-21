package com.example.studyplan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplan.data.StudyNote
import com.example.studyplan.domain.summary.SummaryGenerationException
import com.example.studyplan.domain.summary.SummaryGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Immutable snapshot of the summary-review screen. */
data class SummaryUiState(
    val generatedSummary: String? = null,
    val isGenerating: Boolean = false,
    // Non-null when the last generation attempt failed; holds a message to show.
    val error: String? = null,
)

/**
 * Owns AI-summary generation only. Saving an accepted summary onto a note is a
 * note mutation, so that stays in [NoteViewModel]; the screen wires the two together.
 */
class SummaryViewModel(
    private val summaryGenerator: SummaryGenerator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    fun generate(note: StudyNote) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null, generatedSummary = null) }
            try {
                val summary = summaryGenerator.generateSummary(note)
                _uiState.update { it.copy(generatedSummary = summary) }
            } catch (e: SummaryGenerationException) {
                _uiState.update { it.copy(error = e.message ?: "Failed to generate a summary.") }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun clear() {
        _uiState.update { it.copy(generatedSummary = null, error = null) }
    }
}
