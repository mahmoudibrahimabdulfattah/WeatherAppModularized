package com.mk.skycast.core.ui.text

import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.LocationError
import com.mk.skycast.core.ui.R

fun DataError.toUiText(): UiText = UiText.Resource(
    when (this) {
        DataError.NoInternet -> R.string.core_ui_error_no_internet
        DataError.Timeout -> R.string.core_ui_error_timeout
        is DataError.Server -> R.string.core_ui_error_server
        DataError.Serialization -> R.string.core_ui_error_unexpected
        DataError.NotFound -> R.string.core_ui_error_not_found
        DataError.Unknown -> R.string.core_ui_error_unexpected
    },
)

fun LocationError.toUiText(): UiText = when (this) {
    LocationError.PermissionDenied -> UiText.Resource(R.string.core_ui_error_location_permission)
    LocationError.ProviderDisabled -> UiText.Resource(R.string.core_ui_error_location_disabled)
    LocationError.Unavailable -> UiText.Resource(R.string.core_ui_error_location_unavailable)
    is LocationError.Data -> error.toUiText()
}
