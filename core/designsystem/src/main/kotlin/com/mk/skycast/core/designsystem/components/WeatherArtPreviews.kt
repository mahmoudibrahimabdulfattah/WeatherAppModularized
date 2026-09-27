package com.mk.skycast.core.designsystem.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.designsystem.theme.SkycastTheme

@Preview(name = "Weather art · day", widthDp = 360, heightDp = 360)
@Preview(name = "Weather art · night", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 360)
@Preview(name = "Weather art · RTL", locale = "ar", widthDp = 360, heightDp = 360)
@Composable
private fun WeatherArtPreview() {
    SkycastTheme {
        Box {
            WeatherBackdrop(SkyCondition.Clear, isDay = false, modifier = Modifier.fillMaxSize(), animated = false)
            Column(
                modifier = Modifier.padding(SkySpace.large),
                verticalArrangement = Arrangement.spacedBy(SkySpace.medium),
            ) {
                SkyCondition.entries.chunked(4).forEach { conditions ->
                    GlassCard {
                        Row {
                            conditions.forEach {
                                WeatherIcon(
                                    it,
                                    isDay = true,
                                    modifier = Modifier.weight(1f).height(90.dp),
                                    animated = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
