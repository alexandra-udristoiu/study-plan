package com.example.studyplan.domain.flashcard

import com.google.gson.Gson
import java.time.LocalDate
import kotlin.math.round

class Sm2CardSchedule(
    createdDate: LocalDate = LocalDate.now(),
    lastReviewedDate: LocalDate? = null,
    interval: Int = 0,
    successfulRepetitions: Int = 0,
    easeFactor: Double = INITIAL_EASE,
) : CardSchedule {

    // Baseline a never-reviewed card is due from, so [nextReviewDate] is never null.
    val createdDate: LocalDate = createdDate

    var lastReviewedDate: LocalDate? = lastReviewedDate
        private set

    var interval: Int = interval
        private set

    var successfulRepetitions: Int = successfulRepetitions
        private set

    var easeFactor: Double = easeFactor
        private set

    override val nextReviewDate: LocalDate
        get() = (lastReviewedDate ?: createdDate).plusDays(interval.toLong())

    override fun review(rating: Rating, today: LocalDate): LocalDate {
        // A lapsed card stays due today (interval = 0) and may be re-drilled several
        // times in one session. Only the first review of the day adjusts reps/ease,
        // so repeated Agains don't keep tanking the ease factor.
        if (rating.quality < 3) {
            interval = 0   // due again today
            if (!reviewedEarlierToday(today)) {
                successfulRepetitions = 0
                applyEase(rating)
            }
        } else {
            successfulRepetitions++
            interval = when (successfulRepetitions) {
                1 -> FIRST_INTERVAL
                2 -> SECOND_INTERVAL
                else -> round(interval * easeFactor).toInt()
            }
            applyEase(rating)
        }

        lastReviewedDate = today
        return nextReviewDate
    }

    private fun reviewedEarlierToday(today: LocalDate): Boolean = lastReviewedDate == today

    private fun applyEase(rating: Rating) {
        easeFactor += 0.1 - (5 - rating.quality) * (0.08 + (5 - rating.quality) * 0.02)
        easeFactor = easeFactor.coerceAtLeast(MIN_EASE)
    }

    override fun serialize(): String = GSON.toJson(
        Sm2State(
            createdEpochDay = createdDate.toEpochDay(),
            lastReviewedEpochDay = lastReviewedDate?.toEpochDay(),
            interval = interval,
            successfulRepetitions = successfulRepetitions,
            easeFactor = easeFactor
        )
    )

    companion object {
        const val INITIAL_EASE = 2.5
        const val MIN_EASE = 1.3
        const val FIRST_INTERVAL = 1
        const val SECOND_INTERVAL = 6

        private val GSON = Gson()
    }
}

/** On-disk shape of [Sm2CardSchedule]'s state. Add fields with defaults; never repurpose old ones. */
internal data class Sm2State(
    // Legacy payloads predate this field; Gson leaves it 0 (1970-01-01), which keeps
    // those never-reviewed cards due — matching the old null-means-due behaviour.
    val createdEpochDay: Long = 0,
    val lastReviewedEpochDay: Long? = null,
    val interval: Int = 0,
    val successfulRepetitions: Int = 0,
    val easeFactor: Double = Sm2CardSchedule.INITIAL_EASE
)
