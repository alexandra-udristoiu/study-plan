package com.example.studyplan.domain.flashcard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for the SM-2 scheduling algorithm. Pure domain code, so these run on
 * the host JVM. Dates are passed in explicitly to keep the assertions deterministic.
 */
class Sm2CardScheduleTest {

    private val today: LocalDate = LocalDate.of(2026, 6, 17)

    @Test
    fun newCard_isDueOnItsCreationDate() {
        val schedule = Sm2CardSchedule(createdDate = today)

        // A brand-new card has never been reviewed but must still have a (non-null) due date.
        assertEquals(today, schedule.nextReviewDate)
    }

    @Test
    fun successfulReviews_growTheIntervalOneSixThenEaseScaled() {
        val schedule = Sm2CardSchedule(createdDate = today)

        schedule.review(Rating.GOOD, today)
        assertEquals(1, schedule.interval)

        val secondDay = today.plusDays(1)
        schedule.review(Rating.GOOD, secondDay)
        assertEquals(6, schedule.interval)

        val thirdDay = secondDay.plusDays(6)
        schedule.review(Rating.GOOD, thirdDay)
        // round(6 * 2.5) = 15
        assertEquals(15, schedule.interval)
        assertEquals(thirdDay.plusDays(15), schedule.nextReviewDate)
    }

    @Test
    fun again_keepsCardDueSameDayAndResetsProgress() {
        val schedule = Sm2CardSchedule(createdDate = today)
        schedule.review(Rating.GOOD, today)
        schedule.review(Rating.GOOD, today.plusDays(1))

        val reviewDay = today.plusDays(7)
        schedule.review(Rating.AGAIN, reviewDay)

        assertEquals(0, schedule.interval)
        assertEquals(0, schedule.successfulRepetitions)
        // Lapsed card is due again the same day (never null).
        assertEquals(reviewDay, schedule.nextReviewDate)
    }

    @Test
    fun repeatedAgainOnSameDay_onlyLowersEaseOnce() {
        val schedule = Sm2CardSchedule(createdDate = today)

        schedule.review(Rating.AGAIN, today)
        val easeAfterFirst = schedule.easeFactor
        schedule.review(Rating.AGAIN, today)   // re-drilled the same day

        // The second Again of the day must not tank the ease again.
        assertEquals(easeAfterFirst, schedule.easeFactor, 1e-9)
    }
}
