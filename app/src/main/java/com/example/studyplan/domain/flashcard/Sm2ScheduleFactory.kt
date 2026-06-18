package com.example.studyplan.domain.flashcard

import com.google.gson.Gson
import java.time.LocalDate

/** Produces and restores [Sm2CardSchedule]s. The one SM-2-aware component the app wires in. */
class Sm2ScheduleFactory(private val gson: Gson = Gson()) : CardScheduleFactory {

    override fun newSchedule(): CardSchedule = Sm2CardSchedule()

    override fun fromPayload(payload: String): CardSchedule {
        val state = gson.fromJson(payload, Sm2State::class.java)
        return Sm2CardSchedule(
            createdDate = LocalDate.ofEpochDay(state.createdEpochDay),
            lastReviewedDate = state.lastReviewedEpochDay?.let(LocalDate::ofEpochDay),
            interval = state.interval,
            successfulRepetitions = state.successfulRepetitions,
            easeFactor = state.easeFactor
        )
    }
}
