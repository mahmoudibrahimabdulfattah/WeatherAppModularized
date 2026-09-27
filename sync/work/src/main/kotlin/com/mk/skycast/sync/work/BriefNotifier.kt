package com.mk.skycast.sync.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mk.skycast.core.domain.brief.BriefNotice
import com.mk.skycast.core.domain.usecase.ObserveUserPreferencesUseCase
import com.mk.skycast.core.ui.brief.BriefText
import com.mk.skycast.core.ui.format.WeatherFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Posts (or updates) the single daily brief notification, in the app's chosen language. */
class BriefNotifier @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val observePreferences: ObserveUserPreferencesUseCase,
    private val clock: Clock,
) {
    suspend fun show(notice: BriefNotice) {
        val manager = NotificationManagerCompat.from(appContext)
        val granted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted || !manager.areNotificationsEnabled()) return

        val context = localizedContext()
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.brief_channel_name))
                .setDescription(context.getString(R.string.brief_channel_description))
                .build(),
        )

        val brief = notice.brief
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val formatter =
            WeatherFormatter(
                observePreferences().first(),
                locale,
                android.text.format.DateFormat.is24HourFormat(context),
            )
        val text = BriefText(context.resources, formatter, ZoneId.systemDefault())
        val now = clock.instant()
        val headline = text.headline(brief, now)
        val title = if (notice.isUpdate) context.getString(R.string.brief_update_title, headline) else headline
        val lines = listOfNotNull(text.wear(brief)) +
            brief.hazards.take(MAX_LINES).map(text::hazard) +
            brief.carry.take(MAX_LINES).map(text::carry) +
            listOfNotNull(text.dayOutlook(brief).takeIf { !brief.hasOutings })
        val body = lines.joinToString("\n")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_brief)
            .setContentTitle(title)
            .setContentText(lines.firstOrNull())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp(REQUEST_OPEN, openPlans = false))
            .addAction(0, context.getString(R.string.brief_action_view), openApp(REQUEST_OPEN, openPlans = false))
            .addAction(0, context.getString(R.string.brief_action_plans), openApp(REQUEST_PLANS, openPlans = true))
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    /** Background work gets the device locale; honor the in-app language instead. */
    private fun localizedContext(): Context {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return appContext
        val configuration = Configuration(appContext.resources.configuration)
        configuration.setLocales(android.os.LocaleList.forLanguageTags(locales.toLanguageTags()))
        return appContext.createConfigurationContext(configuration)
    }

    private fun openApp(requestCode: Int, openPlans: Boolean): PendingIntent? {
        val intent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?.putExtra(EXTRA_OPEN_PLANS, openPlans)
            ?: return null
        return PendingIntent.getActivity(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Launch-intent extra asking Home to open "plans changed?". */
        const val EXTRA_OPEN_PLANS = "com.mk.skycast.extra.OPEN_PLANS"
        private const val CHANNEL_ID = "daily_preparation"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN = 1
        private const val REQUEST_PLANS = 2
        private const val MAX_LINES = 3
    }
}
