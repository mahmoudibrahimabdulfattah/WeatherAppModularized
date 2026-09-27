package com.mk.skycast.core.ai

import android.content.Context
import android.content.pm.ApplicationInfo
import android.telephony.TelephonyManager
import com.mk.skycast.core.domain.ai.AiPolicy
import com.mk.skycast.core.domain.ai.AiRegionPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/**
 * Where the device is now: mobile network country, then SIM, then locale as a last resort.
 * Debuggable builds skip the check so emulators (usually "us") can exercise the feature.
 */
internal class TelephonyAiRegionPolicy @Inject constructor(@ApplicationContext private val context: Context) :
    AiRegionPolicy {
    override fun isSupported(): Boolean {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) return true
        val telephony = context.getSystemService(TelephonyManager::class.java)
        val country = telephony?.networkCountryIso?.takeIf { it.isNotBlank() }
            ?: telephony?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().country
        return AiPolicy.isCountryAllowed(country)
    }
}
