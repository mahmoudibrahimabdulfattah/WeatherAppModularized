package com.mk.skycast.core.common

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Source of "now" that keeps time-relative UI (clocks, "updated 3 min ago") live. */
interface TimeTicker {
    /** Emits the current instant immediately, then on every full minute. */
    val ticks: Flow<Instant>
}

class MinuteTicker @Inject constructor(private val clock: Clock) : TimeTicker {
    override val ticks: Flow<Instant> = flow {
        while (true) {
            val now = clock.instant()
            emit(now)
            delay(60_000 - (now.toEpochMilli() % 60_000))
        }
    }
}
