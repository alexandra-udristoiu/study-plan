package com.example.studyplan.data.pdf

/**
 * Thrown when text could not be extracted from a picked PDF — e.g. the file
 * couldn't be read, the backend is unreachable, timed out, or returned an error.
 *
 * Carries a user-presentable [message] so the UI can show it directly.
 */
class PdfExtractionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
