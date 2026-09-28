package com.mk.skycast.core.domain.repository

/** Redraws home-screen widgets after the data behind them changed. */
fun interface WidgetRefresher {
    suspend fun refresh()
}
