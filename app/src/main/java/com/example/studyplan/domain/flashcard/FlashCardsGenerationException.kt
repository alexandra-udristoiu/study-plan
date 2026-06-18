package com.example.studyplan.domain.flashcard

/**
 * Thrown when flashcards could not be generated — e.g. the backend is
 * unreachable, timed out, or returned an error status.
 *
 * Carries a user-presentable [message] so the UI can show it directly.
 */
class FlashCardsGenerationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
