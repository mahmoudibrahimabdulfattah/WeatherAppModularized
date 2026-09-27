package com.mk.skycast.core.common

sealed interface DataError {
    data object NoInternet : DataError
    data object Timeout : DataError
    data class Server(val code: Int) : DataError
    data object Serialization : DataError
    data object NotFound : DataError
    data object Unknown : DataError
}

sealed interface LocationError {
    data object PermissionDenied : LocationError
    data object ProviderDisabled : LocationError
    data object Unavailable : LocationError
    data class Data(val error: DataError) : LocationError
}
