package com.mk.skycast

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mk.skycast.core.designsystem.theme.SkycastTheme
import com.mk.skycast.core.model.ThemeMode
import com.mk.skycast.navigation.SkycastNavHost
import com.mk.skycast.sync.work.BriefNotifier
import dagger.hilt.android.AndroidEntryPoint

/** AppCompatActivity so per-app language changes apply on every supported API level. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Set when the brief notification's "plans changed?" action opened the app. */
    private var openPlans by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.state.value.isLoading }
        openPlans = intent.wantsPlans()

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val darkTheme = when (state.preferences.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                )
                onDispose {}
            }
            SkycastTheme(darkTheme = darkTheme, dynamicColor = state.preferences.useDynamicColor) {
                SkycastNavHost(openPlans = openPlans, onConsumeOpenPlans = { openPlans = false })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.wantsPlans()) openPlans = true
    }

    private fun Intent?.wantsPlans() = this?.getBooleanExtra(BriefNotifier.EXTRA_OPEN_PLANS, false) == true
}
