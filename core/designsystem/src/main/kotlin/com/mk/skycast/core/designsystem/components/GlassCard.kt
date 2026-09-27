package com.mk.skycast.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkySpace

/** Translucent card used on top of [WeatherBackdrop]. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(SkySpace.cardRadius),
        color = SkyColors.Glass,
        contentColor = SkyColors.OnSky,
        border = BorderStroke(1.dp, SkyColors.GlassBorder),
    ) {
        Column(
            modifier = Modifier.padding(SkySpace.medium),
            verticalArrangement = Arrangement.spacedBy(SkySpace.medium),
            content = content,
        )
    }
}

/** Section title exposed to accessibility services as a heading. */
@Composable
fun SkyHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
}
