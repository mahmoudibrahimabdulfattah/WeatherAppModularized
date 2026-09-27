package com.mk.skycast.core.designsystem.components

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.delay

private const val ENTRANCE_DURATION_MS = 380
private const val ENTRANCE_STAGGER_MS = 55L
private const val ENTRANCE_MAX_STAGGERED = 5
private const val ENTRANCE_START_ALPHA = 0.65f
private const val ENTRANCE_OFFSET_DP = 16f
private const val STATIC_PHASE = 0.25f

/**
 * True when the user disabled animations (animator duration scale = 0) or inside
 * previews. Observes the system setting so it reacts without restarting the app.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    val preview = LocalInspectionMode.current
    var reduced by remember(resolver) { mutableStateOf(resolver.animationsDisabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = resolver.animationsDisabled()
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced || preview
}

private fun ContentResolver.animationsDisabled(): Boolean =
    Settings.Global.getFloat(this, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** A looping 0 → 1 phase for ambient animation, or a fixed value when disabled. */
@Composable
internal fun rememberSkyPhase(enabled: Boolean, durationMillis: Int): State<Float> {
    if (!enabled) return remember { mutableFloatStateOf(STATIC_PHASE) }
    return rememberInfiniteTransition(label = "sky").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "sky phase",
    )
}

/**
 * Staggered fade/slide-in for stacked cards. The animation is read in the draw
 * phase only, so [content] is never recomposed by it.
 */
@Composable
fun SkyEntrance(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) {
            progress.snapTo(1f)
        } else {
            delay(index.coerceAtMost(ENTRANCE_MAX_STAGGERED) * ENTRANCE_STAGGER_MS)
            progress.animateTo(1f, tween(ENTRANCE_DURATION_MS, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier.graphicsLayer {
            alpha = ENTRANCE_START_ALPHA + progress.value * (1 - ENTRANCE_START_ALPHA)
            translationY = (1 - progress.value) * ENTRANCE_OFFSET_DP * density
        },
    ) {
        content()
    }
}
