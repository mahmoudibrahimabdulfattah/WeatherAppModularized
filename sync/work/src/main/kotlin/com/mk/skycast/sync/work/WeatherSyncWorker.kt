package com.mk.skycast.sync.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.domain.usecase.RefreshAllWeatherUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WeatherSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val refreshAllWeather: RefreshAllWeatherUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (val result = refreshAllWeather()) {
        is Outcome.Success -> Result.success()

        is Outcome.Failure -> when (result.error) {
            DataError.NoInternet, DataError.Timeout, is DataError.Server ->
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()

            else -> Result.failure()
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}
