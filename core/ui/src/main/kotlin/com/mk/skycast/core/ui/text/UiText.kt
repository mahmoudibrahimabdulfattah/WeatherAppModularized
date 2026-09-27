package com.mk.skycast.core.ui.text

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Text that can be produced by a ViewModel without an Android Context. */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    class Resource(@StringRes val id: Int, vararg val args: Any) : UiText {
        override fun equals(other: Any?) = other is Resource && id == other.id && args.contentEquals(other.args)
        override fun hashCode() = 31 * id + args.contentHashCode()
    }

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Resource -> stringResource(id, *args)
    }

    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(id, *args)
    }
}
