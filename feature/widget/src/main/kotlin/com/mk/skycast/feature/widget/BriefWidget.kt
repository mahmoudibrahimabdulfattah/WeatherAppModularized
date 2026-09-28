package com.mk.skycast.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Home-screen widget: current weather plus the brief's one decision. */
class BriefWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, LARGE))

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun snapshotLoader(): WidgetSnapshotLoader
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loader = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).snapshotLoader()
        val snapshot = loader.load(context)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        provideContent {
            val open = launch?.let { GlanceModifier.clickable(actionStartActivity(it)) } ?: GlanceModifier
            WidgetContent(snapshot, open)
        }
    }

    private companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
        val LARGE = DpSize(250.dp, 180.dp)
    }
}

class BriefWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BriefWidget()
}

private val Background = Color(0xFF0E2E4A)
private val OnSky = ColorProvider(Color(0xFFF6FAFF))
private val MutedSky = ColorProvider(Color(0xFFD4E5EF))

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot, modifier: GlanceModifier = GlanceModifier) {
    val wide = LocalSize.current.width >= 250.dp
    val tall = LocalSize.current.height >= 160.dp
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .cornerRadius(24.dp)
            .padding(14.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                snapshot.locationName?.let {
                    Text(
                        it,
                        maxLines = 1,
                        style = TextStyle(color = OnSky, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    )
                }
                snapshot.condition?.let {
                    Text(it, maxLines = 1, style = TextStyle(color = MutedSky, fontSize = 12.sp))
                }
            }
            snapshot.temperature?.let {
                Text(
                    it,
                    style = TextStyle(
                        color = OnSky,
                        fontSize = if (wide) 34.sp else 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(
            snapshot.headline,
            maxLines = if (wide) 2 else 3,
            style = TextStyle(color = OnSky, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        )
        if (tall) {
            snapshot.detail?.let {
                Spacer(GlanceModifier.height(4.dp))
                Text(it, maxLines = 3, style = TextStyle(color = MutedSky, fontSize = 13.sp))
            }
        }
        if (wide) {
            snapshot.carry?.let {
                Spacer(GlanceModifier.height(4.dp))
                Text(it, maxLines = 1, style = TextStyle(color = MutedSky, fontSize = 12.sp))
            }
        }
    }
}
