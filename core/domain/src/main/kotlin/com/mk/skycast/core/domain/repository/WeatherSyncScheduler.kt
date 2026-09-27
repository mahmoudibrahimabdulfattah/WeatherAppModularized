package com.mk.skycast.core.domain.repository

/** Keeps cached weather fresh in the background. */
interface WeatherSyncScheduler {
    fun schedulePeriodicSync()
    fun requestImmediateSync()
}
